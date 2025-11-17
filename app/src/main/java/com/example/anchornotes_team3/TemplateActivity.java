package com.example.anchornotes_team3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anchornotes_team3.adapter.TemplateAdapter;
import com.example.anchornotes_team3.auth.AuthManager;
import com.example.anchornotes_team3.model.Note;
import com.example.anchornotes_team3.model.Template;
import com.example.anchornotes_team3.repository.NoteRepository;
import com.example.anchornotes_team3.store.ActiveGeofencesStore;
import com.example.anchornotes_team3.util.BottomNavigationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TemplateActivity extends AppCompatActivity {

    private RecyclerView rvTemplates;
    private TextView tvEmpty;
    private FloatingActionButton fabCreateTemplate;

    private NoteRepository noteRepository;
    private AuthManager authManager;
    private ActiveGeofencesStore activeGeofencesStore;
    private TemplateAdapter templateAdapter;
    private List<Template> templates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize repositories and stores
        noteRepository = NoteRepository.getInstance(this);
        authManager = AuthManager.getInstance(this);
        activeGeofencesStore = ActiveGeofencesStore.getInstance(this);

        // Initialize views
        rvTemplates = findViewById(R.id.rv_templates);
        tvEmpty = findViewById(R.id.tv_empty);
        fabCreateTemplate = findViewById(R.id.fab_create_template);

        // Setup bottom navigation
        BottomNavigationHelper.setupBottomNavigation(this, authManager, BottomNavigationHelper.NavItem.TEMPLATES);

        // Initialize adapter
        templates = new ArrayList<>();
        templateAdapter = new TemplateAdapter();
        templateAdapter.setOnTemplateClickListener(new TemplateAdapter.OnTemplateClickListener() {
            @Override
            public void onUseTemplate(Template template) {
                useTemplate(template);
            }

            @Override
            public void onDeleteTemplate(Template template) {
                confirmDeleteTemplate(template);
            }

            @Override
            public void onEditTemplate(Template template) {
                editTemplate(template);
            }
        });
        rvTemplates.setLayoutManager(new LinearLayoutManager(this));
        rvTemplates.setAdapter(templateAdapter);

        // Set up FAB click listener
        fabCreateTemplate.setOnClickListener(v -> showCreateTemplateDialog());
        
        // Set up listener for active geofences (to re-sort when user enters/exits geofences)
        activeGeofencesStore.addListener(activeGeofenceIds -> {
            Log.d("TemplateActivity", "Active geofences changed: " + activeGeofenceIds.size() + " geofences");
            // Re-sort templates when geofences change
            if (templates != null && !templates.isEmpty()) {
                sortAndDisplayTemplates(templates);
            }
        });

        // Load templates
        loadTemplates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload templates when returning to this activity
        loadTemplates();
    }

    private void loadTemplates() {
        noteRepository.getAllTemplates(new NoteRepository.TemplatesCallback() {
            @Override
            public void onSuccess(List<Template> templateList) {
                // Create a new list starting with the example template
                templates = new ArrayList<>();
                templates.add(createExampleTemplate());
                
                // Add user's templates after the example
                if (templateList != null) {
                    templates.addAll(templateList);
                }
                
                // Check proximity to template geofences to ensure ActiveGeofencesStore is up-to-date
                checkProximityToTemplateGeofences(templates);
                
                sortAndDisplayTemplates(templates);
                updateEmptyState();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TemplateActivity.this, "Failed to load templates: " + error, Toast.LENGTH_SHORT).show();
                // Even on error, show the example template
                templates = new ArrayList<>();
                templates.add(createExampleTemplate());
                sortAndDisplayTemplates(templates);
                updateEmptyState();
            }
        });
    }
    
    /**
     * Create a hard-coded example template that appears for all users
     */
    private Template createExampleTemplate() {
        Template example = new Template();
        example.setId("example_template_hardcoded"); // Special ID to identify it
        example.setName("Example Template");
        
        // Create content with formatting: "example content"
        // First word (example) = italic, Second word (content) = bold
        // Using markdown format for formatting
        String formattedContent = "*example* **content**";
        example.setText(formattedContent);
        
        example.setPinned(false);
        
        // Set light brownish background color
        example.setBackgroundColor("#F5E6D3");
        
        // No tags or geofence for example template
        example.setTags(new ArrayList<>());
        example.setGeofence(null);
        
        return example;
    }
    
    /**
     * Check if user is currently near any template geofences and update ActiveGeofencesStore
     */
    private void checkProximityToTemplateGeofences(List<Template> templates) {
        if (templates == null || templates.isEmpty()) {
            return;
        }
        
        // Check if we have location permission
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, 
                android.Manifest.permission.ACCESS_FINE_LOCATION) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.d("TemplateActivity", "No location permission for proximity check");
            return;
        }
        
        // Get templates with geofences
        List<Template> templatesWithGeofences = templates.stream()
            .filter(t -> t.getGeofence() != null)
            .collect(Collectors.toList());
        
        if (templatesWithGeofences.isEmpty()) {
            return;
        }
        
        Log.d("TemplateActivity", "Checking proximity to " + templatesWithGeofences.size() + " template geofences...");
        
        // Get current location
        com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = 
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);
        
        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            null
        ).addOnSuccessListener(location -> {
            if (location != null) {
                Log.d("TemplateActivity", "Current location: " + location.getLatitude() + ", " + location.getLongitude());
                
                // Check distance to each template geofence
                for (Template template : templatesWithGeofences) {
                    com.example.anchornotes_team3.model.Geofence geo = template.getGeofence();
                    float[] distance = new float[1];
                    android.location.Location.distanceBetween(
                        location.getLatitude(), 
                        location.getLongitude(),
                        geo.getLatitude(), 
                        geo.getLongitude(), 
                        distance
                    );
                    
                    if (distance[0] <= geo.getRadius()) {
                        Log.d("TemplateActivity", "✅ Within range of template geofence " + geo.getId() + 
                            " (distance: " + distance[0] + "m, radius: " + geo.getRadius() + "m)");
                        activeGeofencesStore.addActiveGeofence(geo.getId());
                    } else {
                        Log.d("TemplateActivity", "❌ Outside range of template geofence " + geo.getId() + 
                            " (distance: " + distance[0] + "m, radius: " + geo.getRadius() + "m)");
                        // Remove if it was previously active
                        activeGeofencesStore.removeActiveGeofence(geo.getId());
                    }
                }
                
                // After updating active geofences, re-sort templates
                sortAndDisplayTemplates(templates);
            } else {
                Log.w("TemplateActivity", "Could not get current location for proximity check");
            }
        }).addOnFailureListener(e -> {
            Log.e("TemplateActivity", "Failed to get current location for proximity check", e);
        });
    }
    
    /**
     * Sort templates by location relevance and display them
     * Example template always appears first, then location-relevant templates, then others
     */
    private void sortAndDisplayTemplates(List<Template> templateList) {
        if (templateList == null || templateList.isEmpty()) {
            templateAdapter.setTemplates(new ArrayList<>());
            templateAdapter.setActiveGeofenceIds(new HashSet<>());
            return;
        }
        
        // Get currently active geofences
        Set<String> activeGeofences = activeGeofencesStore.getActiveGeofenceIds();
        
        // Sort templates: example template first, then location-relevant ones, then others
        List<Template> sortedTemplates = templateList.stream()
            .sorted((t1, t2) -> {
                boolean t1IsExample = "example_template_hardcoded".equals(t1.getId());
                boolean t2IsExample = "example_template_hardcoded".equals(t2.getId());
                
                // Example template always comes first
                if (t1IsExample && !t2IsExample) {
                    return -1;
                } else if (!t1IsExample && t2IsExample) {
                    return 1;
                }
                
                // For non-example templates, sort by location relevance
                boolean t1HasMatchingGeofence = hasMatchingGeofence(t1, activeGeofences);
                boolean t2HasMatchingGeofence = hasMatchingGeofence(t2, activeGeofences);
                
                // Location-relevant templates come first
                if (t1HasMatchingGeofence && !t2HasMatchingGeofence) {
                    return -1;
                } else if (!t1HasMatchingGeofence && t2HasMatchingGeofence) {
                    return 1;
                } else {
                    // Both same relevance - sort by name
                    String name1 = t1.getName() != null ? t1.getName() : "";
                    String name2 = t2.getName() != null ? t2.getName() : "";
                    return name1.compareToIgnoreCase(name2);
                }
            })
            .collect(Collectors.toList());
        
        Log.d("TemplateActivity", "Sorted " + sortedTemplates.size() + " templates (" + 
            activeGeofences.size() + " active geofences)");
        
        // Update adapter with sorted templates and active geofences
        templateAdapter.setTemplates(sortedTemplates);
        templateAdapter.setActiveGeofenceIds(activeGeofences);
    }
    
    /**
     * Check if template has a geofence matching any active geofence
     */
    private boolean hasMatchingGeofence(Template template, Set<String> activeGeofences) {
        if (template.getGeofence() == null || template.getGeofence().getId() == null) {
            return false;
        }
        return activeGeofences.contains(template.getGeofence().getId());
    }

    private void updateEmptyState() {
        if (templates.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvTemplates.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvTemplates.setVisibility(View.VISIBLE);
        }
    }

    private void showCreateTemplateDialog() {
        // Open note editor in template mode instead of showing dialog
        Intent intent = new Intent(TemplateActivity.this, NoteEditorActivity.class);
        intent.putExtra("is_template_mode", true);
        startActivity(intent);
    }

    private void editTemplate(Template template) {
        if (template == null || template.getId() == null) {
            Toast.makeText(this, "Invalid template", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Don't allow editing the example template
        if ("example_template_hardcoded".equals(template.getId())) {
            Toast.makeText(this, "Cannot edit example template. Use it to create a note instead!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Open note editor in template edit mode
        Intent intent = new Intent(TemplateActivity.this, NoteEditorActivity.class);
        intent.putExtra("is_template_mode", true);
        intent.putExtra("template_id", template.getId());
        intent.putExtra("template_name", template.getName());
        intent.putExtra("template_text", template.getText());
        intent.putExtra("template_pinned", template.getPinned() != null ? template.getPinned() : false);
        
        // Pass tags as ArrayList of tag IDs and names
        if (template.getTags() != null && !template.getTags().isEmpty()) {
            ArrayList<String> tagIds = new ArrayList<>();
            ArrayList<String> tagNames = new ArrayList<>();
            ArrayList<String> tagColors = new ArrayList<>();
            for (com.example.anchornotes_team3.model.Tag tag : template.getTags()) {
                tagIds.add(tag.getId());
                tagNames.add(tag.getName());
                tagColors.add(tag.getColor());
            }
            intent.putStringArrayListExtra("template_tag_ids", tagIds);
            intent.putStringArrayListExtra("template_tag_names", tagNames);
            intent.putStringArrayListExtra("template_tag_colors", tagColors);
        }
        
        // Pass geofence if exists
        if (template.getGeofence() != null) {
            intent.putExtra("template_geofence_id", template.getGeofence().getId());
            intent.putExtra("template_geofence_lat", template.getGeofence().getLatitude());
            intent.putExtra("template_geofence_lng", template.getGeofence().getLongitude());
            intent.putExtra("template_geofence_radius", template.getGeofence().getRadius());
            intent.putExtra("template_geofence_address", template.getGeofence().getAddressName());
        }

        // Pass background color if exists
        if (template.getBackgroundColor() != null) {
            intent.putExtra("template_background_color", template.getBackgroundColor());
        }

        startActivity(intent);
    }

    private void useTemplate(Template template) {
        if (template == null || template.getId() == null) {
            Toast.makeText(this, "Invalid template", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Handle example template specially (it's not in the backend)
        if ("example_template_hardcoded".equals(template.getId())) {
            // Create a new note directly with example template content
            Intent intent = new Intent(TemplateActivity.this, NoteEditorActivity.class);
            // Don't pass note_id - this creates a new note
            intent.putExtra("template_content", template.getText());
            intent.putExtra("template_background_color", template.getBackgroundColor());
            startActivity(intent);
            return;
        }

        // Generate a default note title based on template name
        String defaultTitle = template.getName() != null && !template.getName().isEmpty() 
            ? template.getName() + " (Copy)"
            : "Untitled Note";

        // Instantiate template via backend API
        noteRepository.instantiateTemplate(template.getId(), defaultTitle, new NoteRepository.NoteCallback() {
            @Override
            public void onSuccess(Note note) {
                // Open the new note in editor for editing
                Intent intent = new Intent(TemplateActivity.this, NoteEditorActivity.class);
                intent.putExtra("note_id", note.getId());
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TemplateActivity.this, "Failed to create note: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteTemplate(Template template) {
        if (template == null || template.getId() == null) {
            return;
        }
        
        // Don't allow deleting the example template
        if ("example_template_hardcoded".equals(template.getId())) {
            Toast.makeText(this, "Cannot delete example template", Toast.LENGTH_SHORT).show();
            return;
        }

        String templateName = template.getName();
        if (templateName == null || templateName.trim().isEmpty()) {
            templateName = "Untitled Template";
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_template)
            .setMessage(getString(R.string.delete_template_confirmation) + "\n\n" + templateName)
            .setPositiveButton(R.string.delete_template, (dialog, which) -> {
                deleteTemplate(template.getId());
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void deleteTemplate(String templateId) {
        if (templateId == null || templateId.isEmpty()) {
            return;
        }

        noteRepository.deleteTemplate(templateId, new NoteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(TemplateActivity.this, "Template deleted successfully", Toast.LENGTH_SHORT).show();
                loadTemplates(); // Reload templates
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TemplateActivity.this, "Failed to delete template: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

