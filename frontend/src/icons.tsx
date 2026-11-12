interface FileIconProps {
    extension: string;
    size?: number;
}

const icons = import.meta.glob(
    "/src/assets/icons/latte/*.svg",
    {
        eager: true,
        query: "?url",
        import: "default"
    }
) as Record<string, string>;

export default function FileIcon({ extension, size = 24 }: FileIconProps) {
    const path = `/src/assets/icons/latte/${extension.toLowerCase()}.svg`;
    const src = icons[path];

    if (!src)
        return <img
            src="/src/assets/icons/default_file.svg"
            width={size}
            height={size}
        />;

    return <img src={src} width={size} height={size} />;
}