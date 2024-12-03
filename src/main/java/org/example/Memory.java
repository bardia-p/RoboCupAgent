//
//	File:			Memory.java
//	Author:		    Krzysztof Langner
//	Date:			1997/04/28
//
package org.example;

import java.util.ArrayList;

class Memory {
    //---------------------------------------------------------------------------
    // This constructor:
    // - initializes all variables
    public Memory() {
    }


    //---------------------------------------------------------------------------
    // This function puts see information into our memory
    public void store(VisualInfo info) {
        m_info = info;
    }

    //---------------------------------------------------------------------------
    // This function looks for specified object
    public ObjectInfo getObject(String name) {
        if (m_info == null)
            waitForNewInfo();

        for (int c = 0; c < m_info.m_objects.size(); c++) {
            ObjectInfo object = m_info.m_objects.elementAt(c);
            if (object.m_type.compareTo(name) == 0)
                return object;
        }

        return null;
    }

    public ObjectInfo[] getAllObjects() {
        return m_info.m_objects.toArray(ObjectInfo[]::new);
    }

    //---------------------------------------------------------------------------
    // This function looks for a list of the specified object
    public ArrayList<ObjectInfo> getAll(String name) {
        ArrayList<ObjectInfo> result = new ArrayList<>();
        if (m_info == null)
            waitForNewInfo();

        for (int c = 0; c < m_info.m_objects.size(); c++) {
            ObjectInfo object = m_info.m_objects.elementAt(c);
            if (object.m_type.compareTo(name) == 0)
                result.add(object);
        }

        return result;
    }


    //---------------------------------------------------------------------------
    // This function waits for new visual information
    public void waitForNewInfo() {
        // first remove old info
        m_info = null;
        // now wait until we get new copy
        while (m_info == null) {
            // We can get information faster than 75 milliseconds
            try {
                Thread.sleep(SIMULATOR_STEP);
            } catch (Exception ignored) {
            }
        }
    }


    //===========================================================================
    // Private members
    volatile private VisualInfo m_info;    // place where all information is stored
    final static int SIMULATOR_STEP = 100;
}

