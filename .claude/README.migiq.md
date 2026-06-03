# MigIQ Installation

This directory contains the MigIQ migration platform for Claude Code.

## Installed Components

### Skills (use with /migiq in Claude Code)
- **migiq** - Main orchestration skill
- **mig-graphify** - Code analysis and knowledge graph generation
- **mig-prompt-builder** - Migration requirements builder
- **mig-plan** - Migration planning
- **mig-execute** - Migration execution
- **mig-test-gen** - Test generation
- **mig-containerize** - Container creation
- **mig-deploy** - OpenShift deployment

### Agent (use with Agent tool in Claude Code)
- **migrator** - Autonomous migration agent

## Usage

### Interactive Mode
In any Claude Code session:
```
/migiq
"Migrate this Spring Boot app to Quarkus"
```

### Autonomous Mode
```
Agent({
  description: "Migrate Spring Boot to Quarkus",
  prompt: "Follow AGENT.md. Migrate this Spring Boot application to Quarkus...",
  subagent_type: "general-purpose"
})
```

See agents/migrator/AGENT_EXAMPLES.md for complete examples.

## Documentation
- Main README: https://github.com/sshaaf/migIQ
- Agent Examples: agents/migrator/AGENT_EXAMPLES.md
- Skill Docs: skills/migiq/SKILL.md

## Version
MigIQ v0.2.0

Installation type: local
Installed on: 2026-06-03T09:09:21.348Z
