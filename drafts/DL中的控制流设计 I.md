# DL中的控制流设计

在 `DreamLang V2` 中，控制流语句是实现复杂程序功能的基础。我们需要设计表达力强且语法简明的控制流语句结构，以满足语言的需求。

## 代码块

代码块是所有控制流语句的基础。由于 `DreamLang` 不采用类似 `Python` 的缩进来界定代码块，我们有以下两种方案：

1. 大括号方式
```text
{
    // ... (code)
}
```

2. 关键字方式
```text
begin_token
    // ... (code)
end_token
```

如果希望增加代码可读性，可以牺牲一定开发效率，采用第二种方案，例如：

```lua
do
    // ... (code)
end
```

## 变量作用域

确定了代码块语法后，随之产生的问题是：变量的作用域如何定义？

以下面的Java代码为例：

```java
public static void main(String[] args) {
    {
        String str = new String("Hello");
    }
    System.out.println(str);
}
```

这段代码会产生编译错误：

```text
Scope.java:8: error: cannot find symbol
    System.out.println(str);
                       ^
  symbol:   variable str
  location: class Scope
1 error
error: compilation failed
```

原因是对象 `str` 处于内层代码块（作用域），当代码执行到外层作用域时，无法访问内层作用域的变量。Java中的解决方案是在外层作用域定义一个变量，然后在内层作用域中对其赋值：

```java
public static void main(String[] args) {
    String reference;
    {
        String str = new String("Hello");
        reference = str;
    }
    System.out.println(reference);
}
```

## 作用域访问策略

针对超出作用域访问的情况，我们可以考虑以下两种解决方案：

1. 在外部创建一个相同类型的变量或指针变量并引用

```c
int* ptr;
{
    int x = 114514;
    ptr = &x;
}
int y = 123 + *ptr;
```

2. 创建全局变量

```lua
do
    x = 114514
end
local y = 123 + x
```

虽然第二种方案看似更简单，但其作用域控制粒度较粗：如果我们希望变量只在特定的上级作用域可访问，而在更高层的作用域中不可访问，就需要更精细的控制机制。

比较两种方案的差异：

```c
{
    int* ptr1;
    {
        int x = 114514;
        ptr1 = &x;
    }
    int y = 123 + *ptr1;
}
/*这里无法访问x的值*/
```

```lua
do
    do
        x = 114514
    end
    local y = 123 + x
end
-- 这里仍然能访问x的值
```

## 命名作用域提案

为解决上述问题，我们可以引入命名作用域的概念：

```text
scope outer {
    scope inner {
        var x: int = 114514 available in outer // 在 `outer` 作用域内均可用
    }
    var y: int = 123 + x // 默认作用域为当前作用域
}
```

若需定义全局变量，可以使用：

```text
var global_variable: int = 114514 available in *
```

这里的全局变量类似于 `Java` 和 `Go` 中的包级别作用域。若要实现跨包访问，则需要其他机制支持。

我们甚至还可以丰富一下这个语法的用法：

```text
var x: int = 10 available in ..  // 在父作用域可见
var y: int = 20 available in ... // 在祖父作用域可见
var x: int = 10 available in package  // 模块内可见(相当于 *)
var y: int = 20 available in global  // 全局可(跨包可见)
```

更进一步地，也可以将多个作用域打包称为一个作用域组

```text
scopegroup backend = [models, controllers, services]

var db_connection = connect() available in backend

scope models {
    // ...
}

scope controllers {
    // ...
}

scope services {
    // ...
}
```

如果跳出变量声明周期的语境，这个功能甚至可以用来实现选择性执行功能

```text
scope testtype {
    // doing type testing
}
scope testresult {
    // doing result testing
}
scope benchmark {
    // benchmarking
}
fun test_all(){
    do [testtype, testresult]
}
fun test_bench(){
    do [testtype, testresult, benchmark]
}
```

有一个比较直观的实际案例可以体验一下

```text
// 定义应用的主要作用域结构
scope app {
    // 全局配置，在整个应用中可见
    var config = {
        port: 8080,
        db_url: "localhost:5432/myapp",
        secret_key: "DreamLang",
        debug_mode: true
    } available in *
    
    // 数据库连接池，仅在数据相关作用域可见
    scopegroup data_layer = [models, repositories]
    var connection_pool = init_db_pool(config.db_url) available in data_layer
    
    // 用户会话存储，仅在安全相关作用域可见
    scopegroup security = [auth, session]
    var active_sessions = {} available in security
    
    // 日志系统，在所有作用域可见
    var logger = init_logger(config.debug_mode) available in *
    
    // 模型层
    scope models {
        // 用户模型
        struct User {
            id: int,
            username: string,
            password_hash: string available in auth, // 密码哈希只在auth作用域可见
            email: string,
            is_admin: bool
        }
        
        // 文章模型
        struct Article {
            id: int,
            title: string,
            content: string,
            author_id: int,
            created_at: string
        }
    }
    
    // 仓库层 - 处理数据访问
    scope repositories {
        // 用户仓库
        var user_repo = {
            // 通过ID查找用户
            fun find_by_id(id: int) -> (models.User?) {
                // 可以访问connection_pool因为repositories在data_layer作用域组中
                var result = connection_pool.query("SELECT * FROM users WHERE id = ?", id)
                if (result.is_empty()) {
                    return null
                }
                return result.to_user()
            }
            
            // 保存用户
            fun save(ref user: models.User) -> (bool) {
                // 使用引用参数，可以修改传入的user对象
                if (user.id == 0) {
                    // 新用户，生成ID
                    user.id = generate_id()
                    connection_pool.execute("INSERT INTO users ...")
                } else {
                    // 更新现有用户
                    connection_pool.execute("UPDATE users ...")
                }
                
                logger.info("User saved: " + user.username)
                return true
            }
        }
        
        // 文章仓库
        var article_repo = {
            fun find_recent(limit: int) -> (models.Article[]) {
                return connection_pool.query(
                    "SELECT * FROM articles ORDER BY created_at DESC LIMIT ?", 
                    limit
                ).to_articles()
            }
        }
    }
    
    // 认证层
    scope auth {
        // 登录函数，可以访问User.password_hash
        fun login(username: string, password: string) -> (string?) {
            var user = repositories.user_repo.find_by_username(username)
            if (user == null) {
                logger.warn("Login attempt for non-existent user: " + username)
                return null
            }
            
            if (verify_password(password, user.password_hash)) {
                // 生成会话令牌
                var token = generate_token()
                
                // 存储会话
                active_sessions[token] = user.id
                
                logger.info("User logged in: " + username)
                return token
            }
            
            logger.warn("Failed login attempt for user: " + username)
            return null
        }
        
        // 验证会话
        fun validate_session(token: string) -> (models.User?) {
            if (token in active_sessions) {
                var user_id = active_sessions[token]
                return repositories.user_repo.find_by_id(user_id)
            }
            return null
        }
    }
    
    // 会话管理
    scope session {
        // 获取当前用户
        fun get_current_user(request: Request) -> (models.User?) {
            var token = request.cookies["session_token"]
            if (token == null) {
                return null
            }
            return auth.validate_session(token)
        }
        
        // 需要认证的路由处理器
        fun require_auth(request: Request, response: Response, ref handler: fun(Request, Response, models.User)) {
            var user = get_current_user(request)
            if (user == null) {
                response.redirect("/login")
                return
            }
            
            // 调用原始处理器，传入已认证的用户
            handler(request, response, user)
        }
    }
    
    // 控制器层
    scope controllers {
        // 用户控制器
        var user_controller = {
            // 注册新用户
            fun register(request: Request, response: Response) {
                var form = request.form()
                
                // 创建新用户
                var user = models.User {
                    id: 0,  // 将由save方法生成
                    username: form.username,
                    // 密码哈希在这里创建，但在其他地方不可见
                    password_hash: hash_password(form.password),
                    email: form.email,
                    is_admin: false
                }
                
                // 保存用户 - 注意这里使用引用传递user
                if (repositories.user_repo.save(user)) {
                    // user.id已被修改为新生成的ID
                    logger.info("New user registered with ID: " + user.id)
                    response.redirect("/login")
                } else {
                    response.render("register", {error: "Registration failed"})
                }
            }
            
            // 用户个人资料页面
            fun profile(request: Request, response: Response, user: models.User) {
                // 这个函数通过require_auth调用，确保用户已登录
                
                // 获取用户的文章
                var articles = repositories.article_repo.find_by_author(user.id)
                
                // 渲染个人资料页面
                response.render("profile", {
                    user: user,
                    articles: articles
                })
            }
        }
        
        // 文章控制器
        var article_controller = {
            // 显示首页文章列表
            fun home(request: Request, response: Response) {
                var recent_articles = repositories.article_repo.find_recent(10)
                response.render("home", {articles: recent_articles})
            }
            
            // 创建新文章
            fun create_article(request: Request, response: Response, user: models.User) {
                var form = request.form()
                
                var article = models.Article {
                    id: 0,
                    title: form.title,
                    content: form.content,
                    author_id: user.id,
                    created_at: current_time()
                }
                
                repositories.article_repo.save(article)
                response.redirect("/articles/" + article.id)
            }
        }
    }
    
    // 路由设置
    fun setup_routes(app: Application) {
        // 公开路由
        app.get("/", controllers.article_controller.home)
        app.get("/login", render_login_page)
        app.post("/login", handle_login)
        app.get("/register", render_register_page)
        app.post("/register", controllers.user_controller.register)
        
        // 受保护路由 - 使用session.require_auth包装处理器
        app.get("/profile", (req, res) => {
            session.require_auth(req, res, controllers.user_controller.profile)
        })
        
        app.post("/articles/new", (req, res) => {
            session.require_auth(req, res, controllers.article_controller.create_article)
        })
    }
    
    // 辅助函数
    fun render_login_page(request: Request, response: Response) {
        response.render("login", {})
    }
    
    fun handle_login(request: Request, response: Response) {
        var form = request.form()
        var token = auth.login(form.username, form.password)
        
        if (token != null) {
            response.set_cookie("session_token", token)
            response.redirect("/profile")
        } else {
            response.render("login", {error: "Invalid credentials"})
        }
    }
    
    fun render_register_page(request: Request, response: Response) {
        response.render("register", {})
    }
    
    // 测试作用域
    scope tests {
        // 定义测试组
        scopegroup unit_tests = [model_tests, auth_tests]
        scopegroup integration_tests = [api_tests]
        
        // 模型测试
        scope model_tests {
            fun test_user_model() {
                var user = models.User {
                    id: 1,
                    username: "testuser",
                    password_hash: "hashed_password",
                    email: "test@example.com",
                    is_admin: false
                }
                
                assert(user.username == "testuser")
                assert(user.email == "test@example.com")
                // 无法访问user.password_hash，确保测试不会泄露敏感信息
            }
        }
        
        // 认证测试
        scope auth_tests {
            fun test_login() {
                // 模拟用户
                var user = models.User {
                    id: 999,
                    username: "testuser",
                    password_hash: hash_password("password123"),
                    email: "test@example.com",
                    is_admin: false
                }
                
                // 保存用户
                repositories.user_repo.save(user)
                
                // 测试登录
                var token = auth.login("testuser", "password123")
                assert(token != null)
                
                // 测试错误密码
                var invalid_token = auth.login("testuser", "wrongpassword")
                assert(invalid_token == null)
            }
        }
        
        // 运行所有测试
        fun run_all() {
            do [unit_tests, integration_tests]
        }
        
        // 只运行单元测试
        fun run_unit() {
            do [unit_tests]
        }
    }
}

// 应用入口点
fun main() {
    var application = create_application()
    
    app.setup_routes(application)
    
    // 开发环境下运行测试
    if (app.config.debug_mode) {
        app.tests.run_unit()
    }
    
    // 启动服务器
    application.listen(app.config.port)
    app.logger.info("Server started on port " + app.config.port)
}
```