# neton-application-module-organization

通用【组织】地基模块（moduleId `organization`）：账号（module-account）之上的"组织分组 + 局部角色"能力，
像钉钉里"登录后创建一个自己的组织、组织里放成员"。**与具体业务无关**——设备管理、privchat 工作组等都只
引用 `organization.id` 做归属，组织本身不认识任何业务，也不反向依赖它们。

- 设计规范与 API 见 [SPEC.md](SPEC.md)。
- 对外接缝：`port.OrganizationDirectory`（只读鉴权）、`port.OrganizationExtensions`（生命周期钩子）。
- 平台配对：本后端模块 + `neton-application-front-organization`（后台管理）+ `neton-application-client-organization`（客户端）。

## 独立构建

```bash
./gradlew compileKotlinMacosArm64 -Pnetonstream.local=true -Pnetonstream.local.openssl=true
```

（复合构建下 netonstream 源码替换开关需命令行下发；openssl 未发布到 Maven，走兄弟目录源码。）
