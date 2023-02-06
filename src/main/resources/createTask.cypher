CREATE (p:Task {title: $title, expand: $expand, type: $type, done: $done, estimate: $estimate})
RETURN elementId(p) as `id`, p as `task`;
