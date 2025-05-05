# symbols

本文档描述 `DreamLang V2` 中符号的解析规则

|符号|token (DreamLang V2 Java `./org/dreamlang/Lexical.java`)|优先级|用法|备注|
|---|---|---|---|---|
|`+`|BASIC_OPERATOR|3|`expr + expr (-> expr)`||
|`-`|BASIC_OPERATOR|3|`expr - expr (-> expr)`||
|`*`|BASIC_OPERATOR|2|`expr * expr (-> expr)`||
|`/`|BASIC_OPERATOR|2|`expr / expr (-> expr)`||
|`%`|BASIC_OPERATOR|2|`expr % expr (-> expr)`||
|`^`|BASIC_OPERATOR|2|`expr ^ expr (-> expr)`||
|`++`|ADVANCE_OPERATOR|1|`++var (-> expr)` / `var++ (-> expr)`|增加变量值1|
|`--`|ADVANCE_OPERATOR|1|`--var (-> expr)` / `var-- (-> expr)`|减少变量值1|
|`=`|ASSIGN_OPERATOR|5|`var = expr (-> expr)`|将右侧值赋给左侧变量|
|`+=`|ASSIGN_OPERATOR|5|`var += expr (-> expr)`|加法并赋值|
|`-=`|ASSIGN_OPERATOR|5|`var -= expr (-> expr)`|减法并赋值|
|`*=`|ASSIGN_OPERATOR|5|`var *= expr (-> expr)`|乘法并赋值|
|`/=`|ASSIGN_OPERATOR|5|`var /= expr (-> expr)`|除法并赋值|
|`%=`|ASSIGN_OPERATOR|5|`var %= expr (-> expr)`|取模并赋值|
|`^=`|ASSIGN_OPERATOR|5|`var ^= expr (-> expr)`|幂运算并赋值|
|`==`|LOGICAL_OPERATOR|4|`expr == expr (-> bool)`|比较两值是否相等|
|`>`|LOGICAL_OPERATOR|4|`expr > expr (-> bool)`|左值是否大于右值|
|`<`|LOGICAL_OPERATOR|4|`expr < expr (-> bool)`|左值是否小于右值|
|`>=`|LOGICAL_OPERATOR|4|`expr >= expr (-> bool)`|左值是否大于等于右值|
|`<=`|LOGICAL_OPERATOR|4|`expr <= expr (-> bool)`|左值是否小于等于右值|
|`&&`|SPECICAL_OPERATOR|5|`bool && bool (-> bool)`|两侧条件都为真则结果为真|
|`\|\|`|SPECICAL_OPERATOR|5|`bool \|\| bool (-> bool)`|任一侧条件为真则结果为真|
|`!`|SPECICAL_OPERATOR|1|`!bool (-> bool)`|对条件取反|
|`.`|DOT|1|`object.member (-> member)`|访问对象成员|
|`,`|COMMA|6|`expr, expr (-> expr_list)`|分隔列表中的元素|
|`:`|COLON|5|`key: value (-> entry)`|用于标签或键值对|
|`;`|DIVIDE|6|`stmt; (-> stmt)`|标记语句结束|
|`(`|LEFT_BRACKET|1|`(expr) (-> expr)`|用于函数调用和表达式分组|
|`)`|RIGHT_BRACKET|1|`(expr) (-> expr)`|用于函数调用和表达式分组|
|`[`|LEFT_MPARENTH|1|`array[index] (-> expr)`|用于数组访问和定义|
|`]`|RIGHT_MPARENTH|1|`array[index] (-> expr)`|用于数组访问和定义|
|`{`|LEFT_BRACES|1|`{ stmts } (-> block)`|用于代码块和对象定义|
|`}`|RIGHT_BRACES|1|`{ stmts } (-> block)`|用于代码块和对象定义|