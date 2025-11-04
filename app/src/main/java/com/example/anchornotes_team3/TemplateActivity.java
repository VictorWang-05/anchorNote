package com.example.anchornotes_team3;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TemplateActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView rvTemplates;
    private TextView tvEmpty;
    private FloatingActionButton fabCreateTemplate;

    private NoteRepository noteRepository;
    private TemplateAdapter templateAdapter;
    private List<Template> templates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template);

        // Initialize repositories
        noteRepository = NoteRepository.getInstance(this);

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        rvTemplates = findViewById(R.id.rv_templates);
        tvEmpty = findViewById(R.id.tv_empty);
        fabCreateTemplate = findViewById(R.id.fab_create_template);

        // Set up toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

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
        });
        rvTemplates.setLayoutManager(new LinearLayoutManager(this));
        rvTemplates.setAdapter(templateAdapter);

        // Set up FAB click listener
        fabCreateTemplate.setOnClickListener(v -> showCreateTemplateDialog());

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
                templates = templateList;
                templateAdapter.setTemplates(templates);
                updateEmptyState();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TemplateActivity.this, "Failed to load templates: " + error, Toast.LENGTH_SHORT).show();
                templates = new ArrayList<>();
                templateAdapter.setTemplates(templates);
                updateEmptyState();
            }
        });
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

    private void useTemplate(Template template) {
        if (template == null || template.getId() == null) {
            Toast.makeText(this, "Invalid template", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a default note title based on template name
        String defaultTitle = template.getName() != null && !template.getName().isEmpty() 
            ? template.getName() + " (Copy)"
            : "Untitled Note";

        // Instantiate template directly without showing dialog
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

