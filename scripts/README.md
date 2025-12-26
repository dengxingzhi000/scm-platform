# 脚本工具说明

本目录包含用于POM文件命名规范管理的脚本工具。

## 📋 文件清单

| 文件名 | 说明 | 平台 |
|--------|------|------|
| `check-pom-naming.ps1` | POM命名规范检查脚本 | Windows PowerShell |
| `fix-pom-naming.bat` | POM命名规范自动修复脚本 | Windows |
| `fix-pom-naming.sh` | POM命名规范自动修复脚本 | Linux/macOS |

## 🚀 使用步骤

### 第一步：检查当前POM文件命名规范

**Windows (PowerShell):**
```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-pom-naming.ps1
```

**Linux/macOS:**
```bash
# 安装xmllint工具（如果没有）
# Ubuntu/Debian: sudo apt-get install libxml2-utils
# macOS: brew install libxml2

# 手动检查
grep -r "artifactId" --include="pom.xml" .
```

### 第二步：备份代码

**重要！在执行修复脚本前，请先备份或提交代码到Git：**

```bash
# 查看当前状态
git status

# 创建分支用于POM修复
git checkout -b fix/pom-naming

# 或者创建备份
tar -czf ../scm-platform-backup.tar.gz .
```

### 第三步：执行自动修复

**Windows:**
```cmd
scripts\fix-pom-naming.bat
```

**Linux/macOS:**
```bash
chmod +x scripts/fix-pom-naming.sh
bash scripts/fix-pom-naming.sh
```

### 第四步：验证修复结果

```bash
# 查看修改的文件
git diff

# 重新检查
powershell -ExecutionPolicy Bypass -File scripts\check-pom-naming.ps1
```

### 第五步：测试构建

```bash
# 清理并重新构建
mvn clean install -DskipTests

# 如果构建失败，检查错误信息
mvn clean install
```

### 第六步：提交更改

```bash
# 查看所有修改
git status

# 添加所有修改的POM文件
git add .

# 提交
git commit -m "fix: 统一POM文件命名规范

- 修复scm-common子模块的artifactId命名
- 统一所有模块的groupId为com.frog
- 修复scm-auth和scm-gateway的命名
- 修复scm-system模块的parent引用
- 统一版本号为1.0.0-SNAPSHOT
- 规范化所有name字段

参考文档: docs/POM_NAMING_ISSUES.md"

# 推送到远程（可选）
git push origin fix/pom-naming
```

## 📊 修复内容汇总

### 主要修改项

1. **scm-common 子模块**
   - ❌ Before: `artifactId=core`
   - ✅ After: `artifactId=scm-common-core`
   - 影响模块: core, data, web, integration, monitoring, security-api

2. **Parent引用**
   - ❌ Before: `<groupId>com</groupId><artifactId>NewNearSync</artifactId>`
   - ✅ After: `<groupId>com.frog</groupId><artifactId>scm-common</artifactId>`
   - 影响模块: scm-common所有子模块, scm-system/api

3. **groupId 统一**
   - ❌ Before: `com.frog.common`, `com.frog.auth`, `com.frog.system`
   - ✅ After: `com.frog`
   - 影响: 所有模块

4. **版本号统一**
   - ❌ Before: `1.0-SNAPSHOT`
   - ✅ After: `1.0.0-SNAPSHOT`
   - 影响: 所有依赖引用

5. **name字段规范**
   - ❌ Before: `<name>core</name>`
   - ✅ After: `<name>SCM Common Core</name>`
   - 规则: 有意义的英文描述，首字母大写

## ⚠️ 注意事项

### 1. IDE缓存刷新

修复完成后，需要刷新IDE中的Maven项目：

**IntelliJ IDEA:**
- 右键点击项目根目录 → Maven → Reload Project
- 或点击右侧Maven工具栏的刷新按钮

**Eclipse:**
- 右键点击项目 → Maven → Update Project
- 勾选 "Force Update of Snapshots/Releases"

**VS Code:**
- Ctrl+Shift+P → "Java: Clean Java Language Server Workspace"

### 2. 依赖冲突

如果遇到依赖找不到的错误，检查：

```bash
# 清理本地Maven仓库的旧依赖
rm -rf ~/.m2/repository/com/frog/common/
rm -rf ~/.m2/repository/com/frog/system/

# 重新构建并安装
mvn clean install
```

### 3. 自动化工具

某些IDE插件或工具可能会缓存旧的artifactId，建议：
- 重启IDE
- 清理.idea目录（IntelliJ）
- 清理.project和.classpath（Eclipse）

## 🐛 常见问题

### Q1: 脚本执行后编译失败怎么办？

**A1:** 检查是否所有的dependency引用都已更新：

```bash
# 查找可能遗漏的旧引用
grep -r "com.frog.common" --include="pom.xml" .
grep -r "<artifactId>core</artifactId>" --include="pom.xml" .
```

### Q2: 修改后Git diff显示大量文件怎么办？

**A2:** 这是正常的，因为修复涉及所有POM文件。建议分批次查看：

```bash
# 只查看scm-common相关的修改
git diff -- scm-common/

# 只查看某个文件
git diff scm-auth/pom.xml
```

### Q3: 如何回滚修改？

**A3:** 如果修复出现问题，可以快速回滚：

```bash
# 方法1: 使用Git回滚（如果已提交）
git reset --hard HEAD~1

# 方法2: 丢弃所有未提交的修改
git checkout .

# 方法3: 恢复特定文件
git checkout scm-common/core/pom.xml
```

### Q4: 为什么要统一命名规范？

**A4:** 统一命名规范的好处：
1. **可维护性**: 清晰的命名便于理解模块职责
2. **可扩展性**: 新模块遵循相同规范，保持一致性
3. **自动化**: 便于编写脚本批量处理
4. **团队协作**: 降低沟通成本，减少理解负担
5. **Maven最佳实践**: 符合Maven社区的约定俗成

### Q5: 脚本在Linux上执行报错？

**A5:** 确保脚本有执行权限：

```bash
chmod +x scripts/fix-pom-naming.sh

# 如果仍然报错，检查行尾符
dos2unix scripts/fix-pom-naming.sh

# 或使用sed转换
sed -i 's/\r$//' scripts/fix-pom-naming.sh
```

## 📚 相关文档

- [POM命名规范分析报告](../docs/POM_NAMING_ISSUES.md) - 详细的问题分析和修复建议
- [Maven官方文档](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html)
- [Maven命名约定](https://maven.apache.org/guides/mini/guide-naming-conventions.html)

## 🤝 贡献

如果你发现脚本有bug或需要改进，请：

1. 创建Issue描述问题
2. 提交Pull Request包含修复
3. 更新此README文档

## 📝 更新日志

### 2025-12-25
- ✨ 初始版本
- ✅ 支持Windows和Linux平台
- ✅ 添加自动检查和修复功能
- ✅ 生成详细的分析报告