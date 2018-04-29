# Eclipse-Multiple-Cursor
Eclipse编辑器多点输入插件,支持选中同名字符,选中位置同时输入<br>
本插件源码说明:本插件java代码中只有多点选择类是本人写的,选择下一个同名类是本人修改的之外其余为拷贝[https://github.com/caspark/eclipse-multicursor](https://github.com/caspark/eclipse-multicursor)(原因原作者已经不维护,本人修改了事件的绑定通用性与完善部分功能)
## 基本效果
![gif](https://raw.githubusercontent.com/shenzhenMirren/MyGithubResources/master/image/Eclipse-Multiple-Cursor.gif)
# 使用教程
## 插件使用
将com.szmirren.eclipse.multiple-cursor_1.0.0.jar架包放到Eclipse的plugins文件夹中<br>
## 快捷键说明
选择所有相同的词(所有) : Ctrl+Alt+F (光标先选中词按该快捷键选中所有,输入更改,不支持两个词相连既ssss搜索ss)<br>
选择相同的词(下一个,下一个) : Ctrl+Alt+D (光标先选中词按该快捷键选中下一个相同的词,输入更改,不支持两个词相连既ssss搜索ss)<br>
自定义选择位置(既多个位置) : Ctrl+Alt+V (光标选中将要插入的位置按该快捷键标记)<br>
## 修改快捷键说明
Window - Preferences - General - Keys <br>
Multiple Cursor Select All 对应 选择所有相同的词<br>
Multiple Cursor Select Next 对应 选择相同的词<br>
Multiple Cursor 自定义选择位置 <br>
