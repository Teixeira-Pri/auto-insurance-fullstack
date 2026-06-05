declare module 'react-input-mask' {
  import React from 'react';

  interface InputMaskProps extends React.InputHTMLAttributes<HTMLInputElement> {
    mask: string;
    maskChar?: string | null;
    formatChars?: Record<string, string>;
    alwaysShowMask?: boolean;
    inputRef?: React.Ref<HTMLInputElement>;
    children?: (inputProps: React.InputHTMLAttributes<HTMLInputElement>) => React.ReactNode;
  }

  const InputMask: React.FC<InputMaskProps>;
  export default InputMask;
}
