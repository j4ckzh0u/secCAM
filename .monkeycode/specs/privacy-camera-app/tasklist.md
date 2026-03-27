# 防泄密照相机 APP 实施计划

## 1. 项目初始化

- [ ] 1.1 创建 Gradle Wrapper
   - 生成 gradle wrapper 文件
   - 验证 wrapper 可运行

- [ ] 1.2 配置根目录 build.gradle.kts
   - 配置 Kotlin 版本 1.9.22
   - 配置 Android Gradle Plugin
   - 配置 Hilt、Docker、Compose 插件

- [ ] 1.3 创建 app 模块 build.gradle.kts
   - 配置 compileSdk 34, minSdk 28
   - 添加 Compose、CameraX、ML Kit、Room、Hilt 依赖
   - 配置 Compose Compiler 版本

- [ ] 1.4 创建 AndroidManifest.xml
   - 声明 CAMERA、READ_MEDIA_IMAGES、READ_EXTERNAL_STORAGE 权限

- [ ] 1.5 创建 Application 类
   - 配置 Hilt @HiltAndroidApp
   - 初始化必要的 SDK

## 2. 核心模块创建

- [ ] 2.1 创建 core:security 模块
   - [ ] 2.1.1 实现 EncryptionManager (AES-256-GCM 加密)
   - [ ] 2.1.2 实现 KeystoreManager (Android Keystore 管理)
   - [ ] 2.1.3 实现 PasswordManager (密码验证，PBKDF2)

- [ ] 2.2 创建 core:ui 模块
   - [ ] 2.2.1 实现主题配置 (Compose Theme)
   - [ ] 2.2.2 创建通用按钮组件
   - [ ] 2.2.3 创建通用对话框组件

- [ ] 2.3 创建 domain 模块
   - [ ] 2.3.1 实现 SensitiveInfo 数据类
   - [ ] 2.3.2 实现 SensitiveType 枚举
   - [ ] 2.3.3 实现 ProcessedPhoto 数据类
   - [ ] 2.3.4 实现 AppSettings 数据类
   - [ ] 2.3.5 创建 Repository 接口定义

## 3. 数据层实现

- [ ] 3.1 实现 Room 数据库
   - [ ] 3.1.1 创建 ProcessedPhotoEntity
   - [ ] 3.1.2 创建 AppSettingsEntity
   - [ ] 3.1.3 创建 AppDatabase 类
   - [ ] 3.1.4 创建 DAOs (ProcessedPhotoDao, AppSettingsDao)

- [ ] 3.2 实现 Repository
   - [ ] 3.2.1 实现 PhotoRepositoryImpl
   - [ ] 3.2.2 实现 SettingsRepositoryImpl

- [ ] 3.3 实现 ML 敏感信息检测
   - [ ] 3.3.1 实现 ExifMetadataReader (GPS 信息读取)
   - [ ] 3.3.2 实现 TextRecognitionEngine (ML Kit OCR)
   - [ ] 3.3.3 实现 SensitiveInfoDetector
       - 身份证号正则检测
       - 银行卡号正则检测
       - 手机号正则检测
       - 邮箱正则检测
       - 密码模式检测

## 4. 功能模块实现

- [ ] 4.1 相机模块 (feature:camera)
   - [ ] 4.1.1 创建 CameraScreen
   - [ ] 4.1.2 创建 CameraViewModel
   - [ ] 4.1.3 实现 CameraManager (CameraX 封装)
   - [ ] 4.1.4 实现 SensitiveInfoOverlay (敏感区域高亮)
   - [ ] 4.1.5 实现拍照流程
   - [ ] 4.1.6 实现闪光灯切换
   - [ ] 4.1.7 实现前后摄像头切换

- [ ] 4.2 相册模块 (feature:album)
   - [ ] 4.2.1 创建 AlbumScreen
   - [ ] 4.2.2 创建 AlbumViewModel
   - [ ] 4.2.3 实现 PhotoDetailScreen
   - [ ] 4.2.4 实现 PrivacyAlbumViewModel
   - [ ] 4.2.5 实现密码验证界面
   - [ ] 4.2.6 实现照片网格展示
   - [ ] 4.2.7 实现从相册选择照片

- [ ] 4.3 处理模块
   - [ ] 4.3.1 创建 ProcessingScreen
   - [ ] 4.3.2 创建 ProcessingViewModel
   - [ ] 4.3.3 实现 BlurProcessor (模糊处理)
   - [ ] 4.3.4 实现 PhotoProcessingManager
   - [ ] 4.3.5 实现手动调整模糊区域

- [ ] 4.4 设置模块 (feature:settings)
   - [ ] 4.4.1 创建 SettingsScreen
   - [ ] 4.4.2 创建 SettingsViewModel
   - [ ] 4.4.3 实现自动检测开关
   - [ ] 4.4.4 实现默认处理级别设置
   - [ ] 4.4.5 实现密码修改功能

## 5. 应用集成

- [ ] 5.1 创建 MainActivity
   - [ ] 5.1.1 配置 Compose 入口
   - [ ] 5.1.2 配置权限请求
   - [ ] 5.1.3 配置 Hilt ViewModel

- [ ] 5.2 创建导航结构
   - [ ] 5.2.1 配置 Navigation Compose
   - [ ] 5.2.2 定义路由
   - [ ] 5.2.3 实现底部导航栏

- [ ] 5.3 创建启动器
   - [ ] 5.3.1 实现密码设置界面 (首次启动)
   - [ ] 5.3.2 实现密码验证界面 (非首次启动)
   - [ ] 5.3.3 实现应用锁逻辑

## 6. 检查点
   - 确保项目可以编译通过
   - 确保核心功能可以正常运行

## 7. 收尾工作

- [ ] 7.1 创建 README.md
   - [ ] 7.1.1 编写项目简介
   - [ ] 7.1.2 编写环境配置说明
   - [ ] 7.1.3 编写构建说明

- [ ] 7.2 提交代码到 git
