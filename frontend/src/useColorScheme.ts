import { useSyncExternalStore } from "react";

const query = window.matchMedia("(prefers-color-scheme: dark)");

function subscribe(onChange : () => void) {
    query.addEventListener("change", onChange);
    return () => query.removeEventListener("change", onChange);
}

export type ColorScheme = "light" | "dark";

export default function useColorScheme() : ColorScheme {
    return useSyncExternalStore(subscribe, () => query.matches ? "dark" : "light");
}
