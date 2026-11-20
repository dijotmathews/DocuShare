package com.dijo.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;

/**
 * Error box shown at the top of a form after a failed submit, with an
 * optional action button (e.g. "Edit existing client").
 */
public class ErrorSummary extends Div {

    private final Span text = new Span();
    private final Button action = new Button();
    private Registration actionListener;

    public ErrorSummary() {
        addClassName("form-errors");
        getElement().setAttribute("role", "alert");
        action.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        add(text, action);
        setVisible(false);
    }

    public void show(String message) {
        show(message, null, null);
    }

    public void show(String message, String actionLabel, Runnable onAction) {
        clear();
        text.setText(message);
        action.setVisible(actionLabel != null);
        if (actionLabel != null) {
            action.setText(actionLabel);
            actionListener = action.addClickListener(e -> onAction.run());
        }
        setVisible(true);
    }

    public void clear() {
        setVisible(false);
        text.setText("");
        if (actionListener != null) {
            actionListener.remove();
            actionListener = null;
        }
    }
}
