package org.janelia.it.jacs.shared.utils;

import java.io.Serializable;

public class ControlledVocabElement implements Serializable
{
    public String value;

    public String name;

    public ControlledVocabElement()
    {
    }

    public ControlledVocabElement(String value, String name)
    {
        this.value = value;
        this.name = name;
    }

    public String getValue() { return value; }
    public String getName() { return name; }

    public int hashCode() {
      return name.hashCode();
    }

    public boolean equals(Object otherObject) {
      if (!(otherObject instanceof ControlledVocabElement)) return false;
      ControlledVocabElement other=(ControlledVocabElement) otherObject;
      //Only test the key here for equality.
      if (other.name.equals(name)) return true;
      else return false;
    }
}
