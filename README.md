#APK简要描述
 差异包生成工具

#操作步骤
1. 准备历史版本apk和待发布apk 
    xxx_va.b.c_release.apk
    xxx_vl.m.n_release.apk
    xxx_vx.y.z_release.apk

   其中 xxx_vx.y.z_release.apk 是最新版本APK(待发布apk)

2. 将以上apk放在 ${SDCARD}/DiffAPK 目录, 如果此目录下有其它文件则请事先清空。
    (其中 ${SDCARD} 表示sd卡根目录,根据实际情况填写)

3. 安装完UseBsdiff.apk之后,打开主界面,指定所有apk所在目录路径。

4. 点击"开始生成差异包", 在进行了sdcard读写权限授权成功后,则会在 ${SDCARD}/DiffAPK/dest目录下生成如下差异文件:

    vabc-to-vxyz.patch
    vlmn-to-vxyz.patch

