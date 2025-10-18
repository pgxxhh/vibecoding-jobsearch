export type ClassValue =
  | string
  | number
  | null
  | undefined
  | boolean
  | ClassValue[]
  | { [key: string]: any };

function appendClass(acc: string[], value: ClassValue): void {
  if (!value) return;
  if (typeof value === 'string' || typeof value === 'number') {
    acc.push(String(value));
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((v) => appendClass(acc, v));
    return;
  }
  if (typeof value === 'object') {
    for (const [key, condition] of Object.entries(value)) {
      if (condition) acc.push(key);
    }
  }
}

export function cn(...inputs: ClassValue[]): string {
  const classes: string[] = [];
  inputs.forEach((value) => appendClass(classes, value));
  return classes.join(' ');
}

export default cn;
