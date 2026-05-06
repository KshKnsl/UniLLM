import { cn } from '../../lib/utils';

export function Separator({ className, orientation = 'horizontal', ...props }) {
  return (
    <div
      role="separator"
      aria-orientation={orientation}
      className={cn(
        orientation === 'horizontal' ? 'h-px w-full' : 'h-full w-px',
        'shrink-0 bg-border/80',
        className
      )}
      {...props}
    />
  );
}