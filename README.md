# 作业统计

面向中小学教师的多班级学生作业统计工具。

**单机离线 · 本地优先 · 隐私优先**：无账号、无后端、无遥测，全功能离线可用；学生数据不出本机（除用户主动配置的 WebDAV 云盘备份）。

## 下载

- 安装包：[Releases](https://github.com/Espoirlife/homework-android/releases) 页面下载 APK
- 系统要求：Android 8.0（API 26）及以上
- 侧载安装需在系统设置中开启「允许安装未知来源应用」

## 功能

围绕「布置作业 → 逐生记录 → 统计导出 → 云端备份」闭环设计：

- **班级管理**：多班级卡片列表；学号规则（前缀 + 补零位数 + 回收空号）；一键重排、手动改号并校验重号
- **学生管理**：从 Excel 导入名单（自动识别姓名/备注列，导入前预览、同名去重）；增删改
- **作业录入**：每生三项独立状态——完成（已完成 / 未完成 / 部分完成）、订正（讲解后订正 / 订正完成）、评级（A / B / C / 未评）；单击循环切换、长按直选、批量设置
- **扫码录入**：为学生打印二维码贴在作业本封面，收作业时扫一下即写入预设状态；相同内容 1.5 秒内去重
- **二维码打印**：A4 网格排版，每行个数、页边距、纠错级别可调，四周带虚线裁切框，调用系统打印框架输出
- **统计报表**：单次作业 / 个人两个维度；完成率、待订正、未完成名单实时呈现；导出 `.xlsx`（对齐「作业登记表」版式），或经系统分享发送到金山文档、WPS、微信等
- **备份恢复**：自有格式 JSON 导出 / 导入；WebDAV 自动备份（兼容坚果云、Nextcloud 等），停止操作 30 秒后合并上传，两次自动上传至少间隔 5 分钟；WebDAV 密码经 Android KeyStore 加密存储

## 技术栈

| 层 | 选型 |
| --- | --- |
| 语言 / UI | Kotlin · Jetpack Compose · Material 3 |
| 架构 | MVVM + Repository · Hilt · StateFlow 单向数据流 |
| 本地存储 | Room（SQLite） |
| 导航 | Navigation Compose |
| 扫码 | CameraX + ZXing |
| WebDAV | OkHttp（手写 PROPFIND / PUT / MKCOL 动词，原生直连无 CORS 限制） |
| 凭据加密 | AndroidX Security（EncryptedSharedPreferences） |
| Excel | 手写 XLSX 读写（OOXML / sharedStrings） |

## 项目结构

```
app/src/main/java/com/hwt/teacher/
├── backup/          # WebDAV 自动备份调度（防抖 + 失败静默重试）
├── data/            # Room 实体 / DAO / Repository
├── di/              # Hilt 模块
├── ui/              # Compose 界面（班级 / 作业 / 报表 / 设置 / 扫码 / 二维码打印…）
│   ├── components/  # 通用组件与间距规范
│   └── theme/       # Material 3 主题
├── util/            # XLSX 读写、二维码生成、WebDAV 客户端、备份、分享
└── vm/              # ViewModel
```

## 构建

要求 JDK 17+ 与 Android SDK 34。

```bash
# Windows
gradlew.bat assembleDebug    # 调试包 → debug-apks/hwt-debug-<时间戳>.apk
gradlew.bat assembleRelease  # 正式包 → release-apks/hwt-v<版本号>.apk

# macOS / Linux
./gradlew assembleDebug
./gradlew assembleRelease
```

> 当前 Release 构建复用本机 debug keystore 签名，仅适合自用分发；如需对外正式发布，请替换为自己的签名密钥。

## 权限说明

| 权限 | 用途 |
| --- | --- |
| `CAMERA` | 扫码录入（进入扫码页时运行时申请） |
| `INTERNET` | 仅用于用户主动配置的 WebDAV 备份 |

文件读写经系统 SAF 选择器完成，不申请存储权限；不申请定位、通讯录、通知等任何无关权限。

## 许可

本项目采用 [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html) 许可证开源。
