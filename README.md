# 全平台多媒体播放器整体解决方案(APMP)
All Platform Multimedia Player Solution

开发：贵州广播电视台新媒体中心

当前版本：0.1.1 alpha

## 一、概述
APMP项目架构分为两个层级：基础API库（BAL）、终端实现组件（AC-SDK）。

**BAL:** 向AC-SDK层提供基本功能的接口、第三方服务功能接入和封装、后台相关控制服务，例如HLS直播协议的访问方法、回看功能。

**AC-SDK:** 此层按照平台类型分为三个组件：AAC(对Android平台提供支持)、IAC（对IOS平台提供支持）、HAC（对H5页面相关开发提供支持）。AC层对H5、APP等产品开发所需要的常用功能进行封装，例如视频点播等。

注：限于保密，BAL相关使用不对外公开。

## 二、终端集成和使用

### 1、AAC集成
第一步、gradle依赖
```groovy
    dependencies {
        // jCenter
        implementation 'com.chengfu.fuexoplayer:fuexoplayer:1.9.1'
    }
```

第二步、实例化
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

```java
ExoVideoView exoVideoView = findViewById(R.id.exoVideoView);
exoVideoView.setVideoPath(url);
exoVideoView.start();
```

相关API

### 2、IAC集成和使用

文档正在更新...
