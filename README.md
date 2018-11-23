# FPMF
FPMF is a media playback library with similar APIs to the Android MediaPlayer and VideoView that uses the ExoPlayer as a backing when possible, otherwise the default Android MediaPlayer and VideoView are used.

The ExoPlayer is only supported on devices that pass the compatibility Test Suite and that are JellyBean (API 16) or greater. The ExoPlayer provides additional support for streaming (HLS, DASH, etc.) and full HD (1080p +)

## Use
```groovy
    dependencies {
        // jCenter
        implementation 'com.chengfu.fuexoplayer:fuexoplayer:1.9.1'
    }
```
This library depends ExoPlayer core 2.9.0

## Example
The ExoMedia VideoView can be added in your layout files like any other Android view.

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.chengfu.fuexoplayer.widget.ExoVideoView
        android:id="@+id/exoVideoView"
        android:layout_width="match_parent"
        android:layout_height="200dp">

    </com.chengfu.fuexoplayer.widget.ExoVideoView>

</LinearLayout>
```

While in your Activity or Fragment you treat it like a standard Android VideoView

```java
ExoVideoView exoVideoView = findViewById(R.id.exoVideoView);
exoVideoView.setVideoPath(url);
exoVideoView.start();
```
