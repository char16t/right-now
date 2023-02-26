CREATE (p:Task {title: $title, expand: $expand, type: $type, done: $done, estimate: $estimate, due: $due, startTo: $startTo})
RETURN elementId(p) as `id`, p as `task`;
