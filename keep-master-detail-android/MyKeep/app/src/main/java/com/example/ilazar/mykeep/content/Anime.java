package com.example.ilazar.mykeep.content;

public class Anime {

  public String mId;
  public String mUpdated;
  public String Title;
  public String Synopsis;
  public String NoEpisodes;
  public String Status;
  public String Synonyms;


  public Anime() {
  }

  public Anime(String id, String title,String synopsis, String noEpisodes, String status, String synonyms, String updated) {
    Title = title;
    Synopsis = synopsis;
    NoEpisodes = noEpisodes;
    mId = id;
    mUpdated = updated;
    Synonyms = synonyms;
    Status = status;
  }

  public String getId() {
    return mId;
  }

  public void setId(String id) {
    mId = id;
  }

  public String getUpdated() {
    return mUpdated;
  }

  public void setUpdated(String updated) {
    mUpdated = updated;
  }

  @Override
  public String toString() {
    return "Anime{" +
        "mId='" + mId + '\'' +
        ", mUpdated=" + mUpdated +
        '}';
  }
}
