# Specs — current truth

One directory per capability: `openspec/specs/<capability>/spec.md`.

Format per spec:

```markdown
# Capability: <name>

## Purpose
<one paragraph>

## Requirements

### Requirement: <name>
The system SHALL <behavior>.

#### Scenario: <name>
- WHEN <trigger> THEN <outcome>
```

Deltas proposed by a change (`openspec/changes/<slug>/specs/...`) use
ADDED / MODIFIED / REMOVED and are applied here when the change lands.
