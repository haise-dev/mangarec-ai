import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { PasswordField } from "./PasswordField";

describe("PasswordField", () => {
  it("should render and toggle visibility", () => {
    const onChange = vi.fn();
    render(<PasswordField id="pwd" label="Password" value="secret" onChange={onChange} />);
    
    // jsdom with testing-library might not update the input type correctly or query ByLabelText if not properly linked
    // but we can query by placeholder or id.
    const input = screen.getByLabelText("Password");
    expect(input).toHaveAttribute("type", "password");
    
    const toggleBtn = screen.getByRole("button", { name: "Hiện" });
    fireEvent.click(toggleBtn);
    
    expect(input).toHaveAttribute("type", "text");
    expect(screen.getByRole("button", { name: "Ẩn" })).toBeInTheDocument();
  });

  it("should call onChange on type", () => {
    const onChange = vi.fn();
    render(<PasswordField id="pwd" label="Password" value="" onChange={onChange} />);
    
    const input = screen.getByLabelText("Password");
    fireEvent.change(input, { target: { value: "a" } });
    
    expect(onChange).toHaveBeenCalledWith("a");
  });
});
