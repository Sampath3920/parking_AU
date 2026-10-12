import { useEffect, useState } from "react";
import api from "../services/api";

/**
 * Loads data with GET and loads it again when the path or the params change,
 * or when reload() is called. Pass path = null to load nothing.
 *
 *   const { data, loading, error, reload } = useApi("/admin/users", { page: 0 });
 *
 * "loading" is worked out by comparing what was asked with what has arrived,
 * so no state is set directly inside the effect. While the next answer is
 * loading, the previous data stays available (no flicker).
 */
export default function useApi(path, params) {
    const enabled = Boolean(path);
    const paramsKey = JSON.stringify(params ?? {});
    const [reloadCount, setReloadCount] = useState(0);
    const [result, setResult] = useState({ key: "", data: null, error: "" });

    const key = `${path}|${paramsKey}|${reloadCount}`;

    useEffect(() => {
        if (!enabled) {
            return undefined;
        }
        let cancelled = false;
        api.get(path, { params: JSON.parse(paramsKey) })
            .then((response) => {
                if (!cancelled) {
                    setResult({ key, data: response.data, error: "" });
                }
            })
            .catch((err) => {
                if (!cancelled) {
                    setResult({
                        key,
                        data: null,
                        error: err.response?.data?.message || "Could not load the data"
                    });
                }
            });
        return () => {
            cancelled = true;
        };
    }, [enabled, path, paramsKey, key]);

    return {
        data: enabled ? result.data : null,
        loading: enabled && result.key !== key,
        error: enabled && result.key === key ? result.error : "",
        reload: () => setReloadCount((count) => count + 1)
    };
}
