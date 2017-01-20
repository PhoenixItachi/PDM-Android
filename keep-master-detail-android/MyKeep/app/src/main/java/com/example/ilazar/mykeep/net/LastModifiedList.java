package com.example.ilazar.mykeep.net;

import java.util.Date;
import java.util.List;

public class LastModifiedList<E> {
  private String mLastModified;
  private List<E> mList;

  public LastModifiedList(String lastModified, List<E> list) {
    mLastModified = lastModified;
    mList = list;
  }

  public String getLastModified() {
    return mLastModified;
  }

  public List<E> getList() {
    return mList;
  }
}
