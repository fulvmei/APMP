# 全平台多媒体播放器整体解决方案(APMP)
All Platform Multimedia Player Solution

当前版本：0.1.1 alpha

## 一、概述
APMP项目架构分为两个层级：基础API库（BAL）、终端实现组件（AC-SDK）。

*BAL:* 向AC-SDK层提供基本功能的接口、第三方服务功能接入和封装、后台相关控制服务，例如HLS直播协议的访问方法、回看功能。

*AC-SDK:* 此层按照平台类型分为三个组件：AAC(对Android平台提供支持)、IAC（对IOS平台提供支持）、HAC（对H5页面相关开发提供支持）。AC层对H5、APP等产品开发所需要的常用功能进行封装，例如视频点播等。

注：限于保密，BAL相关使用不对外公开。
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

License
-------
    Copyright 2016-2020 Fu Cheng

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
