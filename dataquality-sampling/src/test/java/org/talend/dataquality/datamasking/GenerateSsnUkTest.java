// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataquality.datamasking;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.talend.dataquality.datamasking.functions.GenerateSsnUk;
import org.talend.dataquality.duplicating.RandomWrapper;

/**
 * created by jgonzalez on 20 août 2015 Detailled comment
 *
 */
public class GenerateSsnUkTest {

    private String output;

    private GenerateSsnUk gsuk = new GenerateSsnUk();

    @Before
    public void setUp() throws Exception {
        gsuk.setRandomWrapper(new RandomWrapper(42));
    }

    @Test
    public void testGood() {
        output = gsuk.generateMaskedRow(null);
        assertEquals(output, "HH 08 07 52 C"); //$NON-NLS-1$
    }

    @Test
    public void testCheck() {
        gsuk.setRandomWrapper(new RandomWrapper());
        boolean res = true;
        for (int i = 0; i < 10; ++i) {
            String tmp = gsuk.generateMaskedRow(null);
            res = gsuk.UPPER.substring(0, 4).indexOf(tmp.charAt(tmp.length() - 1)) != -1
                    && !GenerateSsnUk.getForbid().contains(tmp.substring(0, 2));
            assertEquals("wrong number : " + tmp, res, true); //$NON-NLS-1$
        }
    }

    @Test
    public void testNull() {
        gsuk.keepNull = true;
        output = gsuk.generateMaskedRow(null);
        assertEquals(output, null);
    }
}
