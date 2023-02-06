MATCH (p:Task)
WHERE elementId(p) = $elementId
RETURN p as `task`;
