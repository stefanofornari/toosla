/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package ste.toosla.ui;

import ste.xtest.web.BugFreeWeb;

/**
 *
 */
public class TooslaTestBase extends BugFreeWeb {

    @Override
    protected boolean isPageReady() {
        return "true".equals(String.valueOf(exec("toosla !== undefined && toosla.ready === true")));
    }

}
