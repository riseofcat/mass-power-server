package com.n8cats.share.data;
public class PlayerId {
  @SuppressWarnings("unused") public PlayerId() {
  }
  public PlayerId(int id) {
    this.id = id;
  }
  public int id;
  public int hashCode() {
    return id;
  }
  public boolean equals(Object o) {
    return o != null && (o == this || o.getClass() == PlayerId.class && ((PlayerId) o).id == id);
  }
  public String toString() {
    return String.valueOf(id);
  }
}
