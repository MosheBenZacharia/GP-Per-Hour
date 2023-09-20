/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>, dillydill123 <https://github.com/dillydill123>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gpperhour;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.FlatTextField;

@Slf4j
//Used for session history panel
public class EditableNameField extends JPanel
{
    private static final Border NAME_BOTTOM_BORDER = new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
            BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));

    private String name;
    private final JLabel save = new JLabel("Save");
    private final JLabel cancel = new JLabel("Cancel");
    private final JLabel edit = new JLabel("Edit Name");
    private final MouseAdapter flatTextFieldMouseAdapter;
    @Getter
    private final FlatTextField nameInput = new FlatTextField();
    private Consumer<String> onNameSaved;

    public void setData(String name, Consumer<String> callback)
    {
        if(!name.equals(this.name))
        {
            if (isEditing)
            {
                stopEditing();
            }
            this.name = name;
            nameInput.setText(name);
            updateSaveButtonDuringEditing();
        }
        this.onNameSaved = callback;
    }

    public EditableNameField(final SessionHistoryPanel panel, int maxLength,
            final Color panelColor, final MouseAdapter flatTextFieldMouseAdapter)
    {
        this.flatTextFieldMouseAdapter = flatTextFieldMouseAdapter;
        setLayout(new BorderLayout());

        setBackground(panelColor);
        setBorder(NAME_BOTTOM_BORDER);

        JPanel nameActions = new JPanel(new BorderLayout(3, 0));
        nameActions.setBorder(new EmptyBorder(0, 0, 0, 8));
        nameActions.setBackground(panelColor);

        // Limit character input
        AbstractDocument doc = (AbstractDocument) nameInput.getDocument();
        doc.setDocumentFilter(new DocumentFilter()
        {
            @Override
            public void insertString(FilterBypass fb, int offset, String str, AttributeSet a)
                    throws BadLocationException
            {
                if ((fb.getDocument().getLength() + str.length()) <= maxLength)
                {
                    super.insertString(fb, offset, str, a);
                }
            }

            // Replace handles pasting
            @Override
            public void replace(FilterBypass fb, int offset, int length, String str, AttributeSet a)
                    throws BadLocationException
            {
                if ((fb.getDocument().getLength() + str.length() - length) >= maxLength)
                {
                    // If the user pastes a huge amount of text, cut it out until the maximum length
                    // is achieved
                    int chars_available = maxLength - (fb.getDocument().getLength() - length);
                    int chars_to_cut = str.length() - chars_available;
                    str = str.substring(0, str.length() - chars_to_cut);
                }
                super.replace(fb, offset, length, str, a);
            }
        });

        // Add document listener to disable save button when the name isn't valid
        nameInput.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                updateSaveButtonDuringEditing();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                updateSaveButtonDuringEditing();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                updateSaveButtonDuringEditing();
            }
        });

        nameInput.getTextField().addMouseListener(flatTextFieldMouseAdapter);

        save.setVisible(false);
        save.setFont(FontManager.getRunescapeSmallFont());
        save.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
        save.setBackground(panelColor);
        save.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                if (SwingUtilities.isLeftMouseButton(mouseEvent) && save.isEnabled())
                {
                    name = nameInput.getText();
                    onNameSaved.accept(name);

                    panel.redrawPanels(false);

                    stopEditing();
                }
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent)
            {
                if (!nameInput.getText().isEmpty())
                {
                    save.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR.darker());
                } else
                {
                    save.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
                }
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent)
            {
                if (!nameInput.getText().isEmpty())
                {
                    save.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
                } else
                {
                    save.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
                }
            }
        });

        cancel.setVisible(false);
        cancel.setFont(FontManager.getRunescapeSmallFont());
        cancel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
        cancel.setBackground(panelColor);
        cancel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                if (SwingUtilities.isLeftMouseButton(mouseEvent))
                {
                    nameInput.setText(name);
                    stopEditing();
                }
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent)
            {
                cancel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR.darker());
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent)
            {
                cancel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
            }
        });

        edit.setFont(FontManager.getRunescapeSmallFont());
        edit.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
        edit.setBackground(panelColor);
        edit.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                if (SwingUtilities.isLeftMouseButton(mouseEvent))
                {
                    startEditing();
                }
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent)
            {
                edit.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker().darker());
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent)
            {
                edit.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
            }
        });

        nameActions.add(save, BorderLayout.EAST);
        nameActions.add(cancel, BorderLayout.WEST);
        nameActions.add(edit, BorderLayout.CENTER);

        nameInput.setBorder(null);
        nameInput.setEditable(false);
        nameInput.setBackground(panelColor);
        nameInput.setPreferredSize(new Dimension(0, 24));
        nameInput.getTextField().setForeground(Color.WHITE);
        nameInput.getTextField().setBackground(panelColor);
        nameInput.getTextField().setBorder(new EmptyBorder(0, 6, 0, 0));
        nameInput.getTextField().setCaretPosition(0);

        final JPanel wrapper = new JPanel();
        wrapper.setBackground(panelColor);
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(3, 0, 3, 0));
        wrapper.add(nameInput, BorderLayout.CENTER);
        wrapper.add(nameActions, BorderLayout.EAST);
        add(wrapper, BorderLayout.CENTER);
    }

    private boolean isEditing = false;
    
    private void startEditing()
    {
        isEditing = true;
        // Remove the mouse listener so clicking it doesn't exit the edit screen
        nameInput.getTextField().removeMouseListener(flatTextFieldMouseAdapter);
        nameInput.setEditable(true);
        updateNameActions(true);
    }

    private void stopEditing()
    {
        isEditing = false;
        nameInput.getTextField().addMouseListener(flatTextFieldMouseAdapter);
        nameInput.getTextField().setCaretPosition(0);
        nameInput.setEditable(false);
        updateNameActions(false);
        requestFocusInWindow();
    }

    private void updateNameActions(boolean saveAndCancel)
    {
        save.setVisible(saveAndCancel);
        cancel.setVisible(saveAndCancel);
        edit.setVisible(!saveAndCancel);

        if (saveAndCancel)
        {
            nameInput.getTextField().requestFocusInWindow();
            nameInput.getTextField().selectAll();
        }
    }

    private void updateSaveButtonDuringEditing()
    {
        // If nothing has changed or name is invalid, disable the save button
        if (nameInput.getText().isEmpty() || nameInput.getText().equals(this.name))
        {
            save.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
            save.setEnabled(false);
        } else
        {
            save.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
            save.setEnabled(true);
        }
    }

}
