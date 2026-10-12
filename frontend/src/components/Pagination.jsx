/**
 * Previous / Next buttons for a table. page starts at 0 (like the backend).
 * Shows nothing when everything fits on one page.
 */
function Pagination({ page, totalPages, onChange }) {
    if (!totalPages || totalPages <= 1) {
        return null;
    }

    return (
        <nav
            className="d-flex justify-content-between align-items-center mt-2"
            aria-label="Table pages"
        >
            <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                disabled={page <= 0}
                onClick={() => onChange(page - 1)}
            >
                Previous
            </button>
            <span className="small text-muted" aria-live="polite">
                Page {page + 1} of {totalPages}
            </span>
            <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                disabled={page >= totalPages - 1}
                onClick={() => onChange(page + 1)}
            >
                Next
            </button>
        </nav>
    );
}

export default Pagination;
