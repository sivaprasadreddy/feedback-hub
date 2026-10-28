---
marp: true
theme: default
size: 16:9
paginate: true
header: 'Harness Engineering for Java Developers'
footer: 'K. Siva Prasad Reddy · Developer Advocate @JetBrains'
style: |
  section {
    --navy: #0b152e;
    --panel: #1a2946;
    --yellow: #ffcc33;
    --muted: #e0e6f1;
    --color-highlight: var(--yellow);
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    padding: 96px 64px 88px;
    border-top: 8px solid var(--yellow);
    background: var(--navy);
    color: #ffffff;
    font-family: 'Avenir Next', 'Segoe UI', Arial, sans-serif;
    font-size: 23px;
    line-height: 1.4;
  }
  header {
    position: absolute;
    inset: 34px 64px auto;
    display: flex;
    justify-content: space-between;
    color: var(--muted);
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 0;
  }
  header strong { color: inherit; }
  header em { font-style: normal; }
  h1, h2 {
    margin: 0 0 30px;
    padding: 0;
    border: 0;
    color: #ffffff;
    font-family: Georgia, 'Times New Roman', serif;
    font-weight: 700;
    letter-spacing: -1.5px;
    line-height: 1.12;
  }
  h1 { font-size: 68px; }
  h2 { font-size: 44px; }
  section strong,
  section :is(h1, h2, h3, h4, h5, h6) strong,
  section a,
  section code,
  section mark { color: var(--yellow); }
  section mark { background: transparent; }
  section > ul {
    margin: 0;
    padding-left: 30px;
    list-style: disc;
    color: var(--muted);
    font-size: 26px;
  }
  section > ul > li {
    margin: 0 0 12px;
    padding-left: 8px;
  }
  section li::marker { color: var(--yellow); }
  section > ul > li > p { margin: 0; }
  section > ul ul {
    margin: 6px 0 0;
    padding-left: 30px;
    list-style: circle;
    font-size: 23px;
  }
  section > ul ul li { margin: 0 0 4px; }
  section > ul ul li:last-child { margin-bottom: 0; }
  blockquote {
    margin: 28px 0 0;
    padding: 16px 22px;
    border-left: 5px solid var(--yellow);
    background: var(--panel);
    color: #ffffff;
    font-size: 23px;
    font-weight: 650;
  }
  blockquote p { margin: 0; }
  footer {
    position: absolute;
    inset: auto 64px 24px;
    padding-top: 14px;
    border-top: 1px solid #465169;
    color: var(--muted);
    font-size: 12px;
    line-height: 1.4;
  }
  section::after {
    right: 64px;
    bottom: 24px;
    padding: 0;
    color: #ffffff;
    font-size: 14px;
    font-weight: 750;
    content: attr(data-marpit-pagination) ' / ' attr(data-marpit-pagination-total);
  }
  section.compact h2 { font-size: 37px; }
  section.compact h2 del { color: #aab5c9; }
  section.lead {
    justify-content: center;
    text-align: left;
  }
  section.lead h1 {
    max-width: 1040px;
    margin-bottom: 28px;
    font-size: 68px;
    text-align: left;
  }
  section.lead > p {
    max-width: 960px;
    margin: 0;
    padding: 18px 24px;
    border-left: 5px solid var(--yellow);
    background: var(--panel);
    color: var(--muted);
    font-size: 26px;
  }
---

<!-- _class: lead -->
<!-- _paginate: false -->

# Harness Engineering for **Java Developers**

AI Assisted Development Without Compromising on Quality

---

<!-- _header: 'Harness Engineering for Java Developers *02 / The new normal*' -->

## The New Normal

- **AI Coding Agents**
  - Software Development with AI Coding Agents
  - Models like Opus 5.5, Astra 6.1 are very good
- **Development Workflow**
  - Vibe Coding
  - Vibe Engineering
  - Spec Driven Development
- **Moving towards "Software Factories"**

---

<!-- _class: compact -->
<!-- _header: 'Harness Engineering for Java Developers *03 / Automate routine checks*' -->

## Things You Should ~~Not Spend Time On~~ Automate

- **Linting**
  - CheckStyle
  - SpotBugs · PMD
- **Code Formatting**
  - spring-javaformat
  - Spotless Maven/Gradle Plugin
  - Google/Palantir Java Format
- **Coverage & Quality**
  - Code Coverage and Code Quality Checks
  - JaCoCo, SonarQube

---

<!-- _header: 'Harness Engineering for Java Developers *04 / Architecture verification*' -->

## Automate Architecture Verification

- **Verification Tools**
  - ArchUnit
  - Taikai
- **Architecture Patterns**
  - Layered
  - Onion/Hexagonal
- **Spring Modulith**
  - Module Structure
  - Flat / Nested

---

<!-- _header: 'Harness Engineering for Java Developers *05 / Agent skills*' -->

## Agent Skills

- **Spring Boot Skills**
  - Language/Framework Best Practices
  - Package Structure
  - Testing Guidelines
- **SDD Skills**
  - Requirement -> Spec -> Plan -> Implement -> Verify
  - UseCase Format
  - Loop/Goal Automation

---
<!-- _header: 'Harness Engineering for Java Developers *06 / BootUI advisors*' -->

## BootUI Advisors

- **Developer Console**
  - For Spring Boot & Quarkus
- **Advisors**
  - Inspections for best practices violations
- **MCP**
  - Exposes advisor findings and runtime diagnostics

---

<!-- _class: lead -->
<!-- _header: 'Harness Engineering for Java Developers *07 / Discussion*' -->

# Thank You

Questions?
