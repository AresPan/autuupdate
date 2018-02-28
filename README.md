# autuupdate
android自动更新弹框，下载安装弹框
更新弹框与下载安装
1.	module具体描述
2.	导入方式
3.	弹框设置







































1，	module介绍
该module整合了更新提示，强制更新提示，可选更新提示，更新下载，apk安装等功能。
2，	导入方式
2.1，下载module包，下载地址为： https://github.com/AresPan/autuupdate.git。
2.2，打开AndroidStudio，点击左上角File按键，在弹出的菜单中选择“Import Module”
选项，如下图
 

在弹出的页面中选择刚刚下载的module包，点击确定直至完成
 

编译完成后，打开app下的build gradle页面，添加依赖
“compile project(':autoupdate')”
重新编译

3，	弹框设置
显示弹框方法
create(Activity mContext, boolean isForceUpdate, String mDownloadUrl, String verName, String updateContent)

参数说明
Activity mContext：上下文acticity
boolean isForceUpdate：是否进行强制更新，true为强制更新，false为不强制更新。默认不强制更新。强制更新效果，弹框触摸不消失，点击弹框窗体意外区域，弹框不消失，下载弹框不可取消，不需强制更新反之。
String mDownloadUrl：下载路径，不可为空
String verName：待更新的版本名称
String updateContent：更新的内容
