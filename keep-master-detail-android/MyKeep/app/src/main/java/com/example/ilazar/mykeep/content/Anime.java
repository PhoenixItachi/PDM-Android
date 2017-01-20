package com.example.ilazar.mykeep.content;

public class Anime {

  public enum Status {
    active,
    archived;

  }
  private String mId;
  private String mUserId;
  private String mText;
  private Status mStatus = Status.active;
  private long mUpdated;
  private int mNersion;

  public String Title;
  public String Score;
  public String Synopsis;
  public String NoEpisodes;

  public Anime() {
  }

  public Anime(String title,String score,String synopsis, String noEpisodes) {
    Title = title;
    Score = score;
    Synopsis = synopsis;
    NoEpisodes = noEpisodes;
  }

  public String getId() {
    return mId;
  }

  public void setId(String id) {
    mId = id;
  }

  public String getUserId() {
    return mUserId;
  }

  public void setUserId(String userId) {
    mUserId = userId;
  }

  public String getText() {
    return mText;
  }

  public void setText(String text) {
    mText = text;
  }

  public Status getStatus() {
    return mStatus;
  }

  public void setStatus(Status status) {
    mStatus = status;
  }

  public long getUpdated() {
    return mUpdated;
  }

  public void setUpdated(long updated) {
    mUpdated = updated;
  }

  public int getVersion() {
    return mNersion;
  }

  public void setVersion(int version) {
    mNersion = version;
  }

  @Override
  public String toString() {
    return "Anime{" +
        "mId='" + mId + '\'' +
        ", mUserId='" + mUserId + '\'' +
        ", mText='" + mText + '\'' +
        ", mStatus=" + mStatus +
        ", mUpdated=" + mUpdated +
        ", mNersion=" + mNersion +
        '}';
  }
}
