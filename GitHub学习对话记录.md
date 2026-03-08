# GitHub 与 Git 学习对话记录

> 本文档记录学习 GitHub 过程中的问答与要点，便于日后查阅。

---

## 目录

1. [GitHub 功能概述](#1-github-功能概述)
2. [从零开始学习 GitHub](#2-从零开始学习-github)
3. [Git 安装详解（第四步）](#3-git-安装详解第四步)
4. [Git 版本与更新](#4-git-版本与更新)
5. [本地 Git 与 GitHub 账号关联](#5-本地-git-与-github-账号关联)
6. [Git 默认编辑器设置](#6-git-默认编辑器设置)
7. [克隆路径与存储位置](#7-克隆路径与存储位置)
8. [git add / commit / push 含义](#8-git-add--commit--push-含义)
9. [一键提交：git acp 别名](#9-一键提交git-acp-别名)
10. [分支的作用与使用](#10-分支的作用与使用)
11. [代理配置与网络问题](#11-代理配置与网络问题)
12. [分支合并与 git revert](#12-分支合并与-git-revert)
13. [Git 与计算机专业](#13-git-与计算机专业)

---

## 1. GitHub 功能概述

- **代码托管**：仓库、版本控制、提交历史、分支管理
- **协作开发**：Pull Request、Code Review、Issue、讨论区
- **项目管理**：Projects、Milestones、Labels
- **自动化**：GitHub Actions、CI/CD
- **安全**：Dependabot、Code Scanning、私有仓库
- **文档**：README、GitHub Pages、Wiki、Release

---

## 2. 从零开始学习 GitHub

### 学习步骤概览

1. **注册账号**：github.com → Sign up
2. **创建仓库**：New repository，勾选 README
3. **安装 Git**：git-scm.com
4. **配置 Git**：user.name、user.email
5. **克隆仓库**：`git clone <仓库地址>`
6. **修改并推送**：`git add .` → `git commit -m "说明"` → `git push`

---

## 3. Git 安装详解（第四步）

### macOS 安装方式

- **方式 1**：输入 `git --version`，未安装时会提示安装 Xcode 命令行工具
- **方式 2**：`brew install git`
- **方式 3**：官网下载 .dmg 安装包

### 必做配置

```bash
git config --global user.name "你的名字"
git config --global user.email "你的邮箱"
```

### 可选配置

```bash
git config --global init.defaultBranch main
git config --global core.quotepath false
```

---

## 4. Git 版本与更新

- 当前版本：Git 2.50.1
- 结论：**不需要更新**，版本足够新，满足日常使用

---

## 5. 本地 Git 与 GitHub 账号关联

| 配置项 | 是否需与 GitHub 一致 |
|--------|----------------------|
| user.email | **建议一致**，否则提交无法关联到 GitHub 账号 |
| user.name | 建议一致，便于识别 |

- 邮箱用于 GitHub 识别提交者身份
- 可使用 GitHub 隐私邮箱：`xxx@users.noreply.github.com`

---

## 6. Git 默认编辑器设置

- 使用 Cursor：`git config --global core.editor "cursor --wait"`
- 需先在 Cursor 中：Cmd+Shift+P → 安装 cursor 命令
- **新手建议**：不配置，直接用 `git commit -m "说明"` 即可

---

## 7. 克隆路径与存储位置

- `cd ~/Desktop` 只是示例，**不一定要放桌面**
- 可放在拓展坞硬盘：`cd /Volumes/存钱罐/cursor项目`
- 项目会克隆到当前目录下的子文件夹

---

## 8. git add / commit / push 含义

| 命令 | 作用 |
|------|------|
| `git add .` | 把修改加入暂存区，选择要提交的内容 |
| `git commit -m "说明"` | 在本地创建提交，保存当前改动 |
| `git push` | 把本地提交推送到 GitHub |

**流程**：工作区 → 暂存区 → 本地仓库 → 远程仓库

---

## 9. 一键提交：git acp 别名

### 配置命令

```bash
git config --global alias.acp '!f() { git add . && git commit -m "$1" && git push; }; f'
```

### 使用方式

```bash
git acp "你的提交说明"
```

等价于：`git add .` + `git commit -m "说明"` + `git push`

---

## 10. 分支的作用与使用

### 核心作用

- **隔离**：不同工作在不同分支，互不影响
- **安全**：main 保持稳定，新功能在分支开发
- **协作**：多人可同时在不同分支开发

### 常用命令

```bash
git checkout -b 分支名    # 创建并切换分支
git branch               # 查看分支（* 表示当前）
git push -u origin 分支名 # 推送新分支到 GitHub
git checkout main        # 切回主分支
git merge 分支名          # 合并分支到当前分支
```

### 分支不会创建新文件夹

- 分支是同一套文件的不同"版本"
- 在 Cursor 左下角可看到当前分支
- 新分支需 `git push -u origin 分支名` 才会出现在 GitHub

---

## 11. 代理配置与网络问题

### 常见错误

```
fatal: unable to access '...': Recv failure: Operation timed out
Failed to connect to github.com port 443
```

### 原因

- 浏览器能访问 GitHub（走代理）
- Git 命令行默认不走代理，需单独配置

### 解决方案

代理软件显示端口（如 HTTP:33210），执行：

```bash
git config --global http.https://github.com.proxy http://127.0.0.1:33210
git config --global https.https://github.com.proxy http://127.0.0.1:33210
```

### 代理经常切换

- 将「节点线路」从「自动选择」改为**手动选择固定节点**
- 减少连接不稳定

---

## 12. 分支合并与 git revert

### 重要：删除分支 ≠ 撤销合并

- 合并后，改动已写入 main 的历史
- 删除分支只是删除分支指针，**不会让 main 回到合并前**

### git revert 如何撤销

- **原理**：新建一个提交，把目标提交的改动**反过来做一遍**
- **不删除历史**：保留完整记录，多一个 revert 提交
- **撤销合并**：`git revert -m 1 <合并提交ID>`

### 与 git reset 区别

| | git revert | git reset |
|---|------------|-----------|
| 方式 | 新建反向提交 | 移动指针，丢弃提交 |
| 历史 | 保留 | 改写 |
| 适用 | 已推送的提交 | 未推送的提交 |

---

## 13. Git 与计算机专业

- **算基础知识**：是，实际工作中必备
- **学校是否教**：因校而异，很多靠自学
- **行业地位**：求职、工作、开源几乎都会用到

---

## 附录：常用命令速查

```bash
git status          # 查看状态
git log             # 查看提交历史
git branch          # 查看分支
git pull            # 拉取远程更新
git acp "说明"      # 一键 add + commit + push
```

---测试

## 2026年3月9日 对话记录

**用户问题**：能否自动把对话加到记录里，容易忘记

**要点**：
- 已创建 Cursor 规则：`.cursor/rules/github-learning-record.mdc`
- 规则设置为 `alwaysApply: true`，每次对话都会生效
- 当讨论 GitHub/Git 相关话题时，会自动将要点追加到本文件
- 无需手动提醒，AI 会在回复结束前自动更新

---

*文档创建时间：2026年3月*
*持续更新中...*
