module com.udacity.catpoint {
    requires miglayout;
    requires com.google.common;
    requires com.google.gson;
    requires java.desktop;
    requires java.prefs;
    requires com.udacity.catpoint.image;

    opens com.udacity.catpoint.data to com.google.gson;
}