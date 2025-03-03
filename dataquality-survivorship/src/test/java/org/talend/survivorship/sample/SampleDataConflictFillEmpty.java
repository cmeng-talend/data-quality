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
package org.talend.survivorship.sample;

import org.talend.survivorship.model.ConflictRuleDefinition;
import org.talend.survivorship.model.DefFunParameter;
import org.talend.survivorship.model.RuleDefinition;
import org.talend.survivorship.model.RuleDefinition.Function;
import org.talend.survivorship.model.RuleDefinition.Order;

public class SampleDataConflictFillEmpty {

    public static final String PKG_NAME_CONFLICT = "org.talend.survivorship.conflict.fill_empty"; //$NON-NLS-1$

    public static final RuleDefinition[] RULES_CONFLICT = { new RuleDefinition(Order.SEQ, "R1", "lastName", //$NON-NLS-1$ //$NON-NLS-2$
            Function.MostCommon, null, "lastName", false) }; //$NON-NLS-1$

    public static final ConflictRuleDefinition[] RULES_CONFLICT_RESOLVE = {

            new ConflictRuleDefinition(new DefFunParameter("firstName", Function.FillEmpty, null, "lastName", null), Order.CR, //$NON-NLS-1$//$NON-NLS-2$
                    "CR1", false, false, 0),
            new ConflictRuleDefinition(new DefFunParameter("lastName", Function.Longest, null, "lastName", null), Order.CR, "CR2", //$NON-NLS-1$ //$NON-NLS-2$
                    false, false, 1) };
}
