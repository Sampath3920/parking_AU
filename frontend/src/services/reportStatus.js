/** Words and colours for the status of a complaint or damage report. */
export const STATUS_LABEL = {
    SUBMITTED: "Submitted",
    IN_PROGRESS: "In progress",
    RESOLVED: "Resolved",
    REJECTED: "Rejected"
};

export function statusBadge(status) {
    if (status === "RESOLVED") return "bg-success";
    if (status === "REJECTED") return "bg-danger";
    if (status === "IN_PROGRESS") return "bg-info text-dark";
    return "bg-warning text-dark";
}
