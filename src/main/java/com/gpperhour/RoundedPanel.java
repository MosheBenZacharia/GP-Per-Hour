/*
 * Copyright (c) 2023, Moshe Ben-Zacharia <https://github.com/MosheBenZacharia>
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

import javax.swing.*;
import java.awt.*;

//Custom panel for rounded edges on the top left and top right corners.
public class RoundedPanel extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        int arc = 16; // Adjust the arc value to control the roundness of the corners
        int borderWidth = 0; 

        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(getBackground());
        g2d.fillArc(borderWidth, borderWidth, arc, arc, 90, 90);
        g2d.fillArc(getWidth() - (borderWidth + arc), borderWidth, arc, arc, 0, 90);
        g2d.fillRect(borderWidth+(arc/2), borderWidth, getWidth() - (2*borderWidth + arc), arc/2);
        g2d.fillRect(borderWidth, arc/2 + borderWidth, getWidth() - 2*borderWidth, getHeight() - (borderWidth + arc/2));

        g2d.setColor(getForeground());
        g2d.drawArc(borderWidth, borderWidth, arc, arc, 90, 90);
        g2d.drawArc(getWidth() - (borderWidth + arc), borderWidth, arc, arc, 0, 90);
        g2d.drawLine(borderWidth + arc/2, borderWidth, getWidth() - arc/2, borderWidth);
        g2d.drawLine(borderWidth, arc/3 + borderWidth, borderWidth, getHeight() - (borderWidth));
        g2d.drawLine(getWidth()- borderWidth-1, arc/3 + borderWidth, getWidth() - borderWidth-1, getHeight() - (borderWidth));

        g2d.dispose();
    }
}