module com.udacity.catpoint.security {
    requires miglayout;
    requires com.google.common;
    requires com.google.gson;
    requires java.desktop;
    requires java.prefs;
    requires com.udacity.catpoint.image;
    requires java.sql;
    opens com.udacity.catpoint.security.data to com.google.gson;
}