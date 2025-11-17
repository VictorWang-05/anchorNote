package com.example.anchornotes_team3.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class for filter criteria
 * Implements Parcelable to pass between activities
 */
public class FilterCriteria implements Parcelable {
    private List<String> tagIds;
    private Boolean pinned;
    private Boolean hasPhoto;
    private Boolean hasAudio;
    private Boolean hasLocation;
    // Optional last-edited range (epoch millis, inclusive)
    private Long lastEditedFromMs;
    private Long lastEditedToMs;

    public FilterCriteria() {
        this.tagIds = new ArrayList<>();
        this.pinned = null;
        this.hasPhoto = null;
        this.hasAudio = null;
        this.hasLocation = null;
        this.lastEditedFromMs = null;
        this.lastEditedToMs = null;
    }

    protected FilterCriteria(Parcel in) {
        tagIds = in.createStringArrayList();
        byte tmpPinned = in.readByte();
        pinned = tmpPinned == 0 ? null : (tmpPinned == 1);
        byte tmpHasPhoto = in.readByte();
        hasPhoto = tmpHasPhoto == 0 ? null : (tmpHasPhoto == 1);
        byte tmpHasAudio = in.readByte();
        hasAudio = tmpHasAudio == 0 ? null : (tmpHasAudio == 1);
        byte tmpHasLocation = in.readByte();
        hasLocation = tmpHasLocation == 0 ? null : (tmpHasLocation == 1);
        lastEditedFromMs = (Long) in.readValue(Long.class.getClassLoader());
        lastEditedToMs = (Long) in.readValue(Long.class.getClassLoader());
    }

    public static final Creator<FilterCriteria> CREATOR = new Creator<FilterCriteria>() {
        @Override
        public FilterCriteria createFromParcel(Parcel in) {
            return new FilterCriteria(in);
        }

        @Override
        public FilterCriteria[] newArray(int size) {
            return new FilterCriteria[size];
        }
    };

    public List<String> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<String> tagIds) {
        this.tagIds = tagIds;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public Boolean getHasPhoto() {
        return hasPhoto;
    }

    public void setHasPhoto(Boolean hasPhoto) {
        this.hasPhoto = hasPhoto;
    }

    public Boolean getHasAudio() {
        return hasAudio;
    }

    public void setHasAudio(Boolean hasAudio) {
        this.hasAudio = hasAudio;
    }

    public Boolean getHasLocation() {
        return hasLocation;
    }

    public void setHasLocation(Boolean hasLocation) {
        this.hasLocation = hasLocation;
    }

    public Long getLastEditedFromMs() {
        return lastEditedFromMs;
    }

    public void setLastEditedFromMs(Long lastEditedFromMs) {
        this.lastEditedFromMs = lastEditedFromMs;
    }

    public Long getLastEditedToMs() {
        return lastEditedToMs;
    }

    public void setLastEditedToMs(Long lastEditedToMs) {
        this.lastEditedToMs = lastEditedToMs;
    }

    public boolean isEmpty() {
        return (tagIds == null || tagIds.isEmpty()) &&
                pinned == null &&
                hasPhoto == null &&
                hasAudio == null &&
                hasLocation == null &&
                lastEditedFromMs == null &&
                lastEditedToMs == null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(tagIds);
        dest.writeByte((byte) (pinned == null ? 0 : pinned ? 1 : 2));
        dest.writeByte((byte) (hasPhoto == null ? 0 : hasPhoto ? 1 : 2));
        dest.writeByte((byte) (hasAudio == null ? 0 : hasAudio ? 1 : 2));
        dest.writeByte((byte) (hasLocation == null ? 0 : hasLocation ? 1 : 2));
        dest.writeValue(lastEditedFromMs);
        dest.writeValue(lastEditedToMs);
    }
}
