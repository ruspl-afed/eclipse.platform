/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.expressions.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.expressions.IVariableResolver;

import org.eclipse.core.internal.expressions.AdaptExpression;
import org.eclipse.core.internal.expressions.AndExpression;
import org.eclipse.core.internal.expressions.CountExpression;
import org.eclipse.core.internal.expressions.EnablementExpression;
import org.eclipse.core.internal.expressions.EqualsExpression;
import org.eclipse.core.internal.expressions.Expressions;
import org.eclipse.core.internal.expressions.InstanceofExpression;
import org.eclipse.core.internal.expressions.IterateExpression;
import org.eclipse.core.internal.expressions.NotExpression;
import org.eclipse.core.internal.expressions.OrExpression;
import org.eclipse.core.internal.expressions.ResolveExpression;
import org.eclipse.core.internal.expressions.SystemTestExpression;
import org.eclipse.core.internal.expressions.TestExpression;
import org.eclipse.core.internal.expressions.WithExpression;


public class ExpressionTests extends TestCase {

	public static Test suite() {
		return new TestSuite(ExpressionTests.class);
	}
	
	public void testEscape() throws Exception {
		assertEquals("Str'ing", Expressions.unEscapeString("Str''ing")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("'", Expressions.unEscapeString("''")); //$NON-NLS-1$ //$NON-NLS-2$
		boolean caught= false;
		try {
			Expressions.unEscapeString("'"); //$NON-NLS-1$
		} catch (CoreException e) {
			caught= true;
		}
		assertTrue(caught);
	}
	
	public void testArgumentConversion() throws Exception {
		assertNull(Expressions.convertArgument(null));
		assertEquals("", Expressions.convertArgument("")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("", Expressions.convertArgument("''")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("eclipse", Expressions.convertArgument("eclipse")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("e'clips'e", Expressions.convertArgument("e'clips'e")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("eclipse", Expressions.convertArgument("'eclipse'")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("'ecl'ipse'", Expressions.convertArgument("'''ecl''ipse'''")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("true", Expressions.convertArgument("'true'")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.7", Expressions.convertArgument("'1.7'")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("007", Expressions.convertArgument("'007'")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Boolean.TRUE, Expressions.convertArgument("true")); //$NON-NLS-1$
		assertEquals(Boolean.FALSE, Expressions.convertArgument("false")); //$NON-NLS-1$
		assertEquals(new Integer(100), Expressions.convertArgument("100")); //$NON-NLS-1$
		assertEquals(new Float(1.7f), Expressions.convertArgument("1.7")); //$NON-NLS-1$
	}
	
	public void testArgumentParsing() throws Exception {
		Object[] result= null;
		
		result= Expressions.parseArguments(""); //$NON-NLS-1$
		assertEquals("", result[0]); //$NON-NLS-1$
		
		result= Expressions.parseArguments("s1"); //$NON-NLS-1$
		assertEquals("s1", result[0]); //$NON-NLS-1$
		
		result= Expressions.parseArguments(" s1 "); //$NON-NLS-1$
		assertEquals("s1", result[0]); //$NON-NLS-1$
		
		result= Expressions.parseArguments("s1,s2"); //$NON-NLS-1$
		assertEquals("s1", result[0]); //$NON-NLS-1$
		assertEquals("s2", result[1]); //$NON-NLS-1$
		
		result= Expressions.parseArguments(" s1 , s2 "); //$NON-NLS-1$
		assertEquals("s1", result[0]); //$NON-NLS-1$
		assertEquals("s2", result[1]); //$NON-NLS-1$
		
		result= Expressions.parseArguments("' s1 ',' s2 '"); //$NON-NLS-1$
		assertEquals(" s1 ", result[0]); //$NON-NLS-1$
		assertEquals(" s2 ", result[1]); //$NON-NLS-1$
		
		result= Expressions.parseArguments(" s1 , ' s2 '"); //$NON-NLS-1$
		assertEquals("s1", result[0]); //$NON-NLS-1$
		assertEquals(" s2 ", result[1]); //$NON-NLS-1$
		
		result= Expressions.parseArguments("' s1 ', s2 "); //$NON-NLS-1$
		assertEquals(" s1 ", result[0]); //$NON-NLS-1$
		assertEquals("s2", result[1]); //$NON-NLS-1$
		
		result= Expressions.parseArguments("''''"); //$NON-NLS-1$
		assertEquals("'", result[0]); //$NON-NLS-1$
		
		result= Expressions.parseArguments("''',''',','"); //$NON-NLS-1$
		assertEquals("','", result[0]);		 //$NON-NLS-1$
		assertEquals(",", result[1]); //$NON-NLS-1$
		
		result= Expressions.parseArguments("' s1 ', true "); //$NON-NLS-1$
		assertEquals(" s1 ", result[0]); //$NON-NLS-1$
		assertEquals(Boolean.TRUE, result[1]);
		
		boolean caught= false;
		try {
			Expressions.parseArguments("' s1"); //$NON-NLS-1$
		} catch (CoreException e) {
			caught= true;
		}
		assertTrue(caught);
		caught= false;
		try {
			Expressions.parseArguments("'''s1"); //$NON-NLS-1$
		} catch (CoreException e) {
			caught= true;
		}
		assertTrue(caught);
	}
	
	public void testSystemProperty() throws Exception {
		SystemTestExpression expression= new SystemTestExpression("os.name", System.getProperty("os.name")); //$NON-NLS-1$ //$NON-NLS-2$
		EvaluationResult result= expression.evaluate(new EvaluationContext(null, new Object()));
		assertTrue(result == EvaluationResult.TRUE);
	}
	
	public void testAdaptExpression() throws Exception {
		AdaptExpression expression= new AdaptExpression("org.eclipse.core.internal.expressions.tests.Adapter"); //$NON-NLS-1$
		expression.add(new InstanceofExpression("org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		EvaluationResult result= expression.evaluate(new EvaluationContext(null, new Adaptee()));
		assertTrue(result == EvaluationResult.TRUE);
	}

	public void testAdaptExpressionNotEqual() throws Exception {
		AdaptExpression expression1 = new AdaptExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter"); //$NON-NLS-1$
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter1")); //$NON-NLS-1$
		AdaptExpression expression2 = new AdaptExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter"); //$NON-NLS-1$
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter2")); //$NON-NLS-1$
		assertTrue("These adapt expressions should not be equal", !expression1
				.equals(expression2));
	}

	public void testAdaptExpressionHashCode() throws Exception {
		AdaptExpression expression1 = new AdaptExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter"); //$NON-NLS-1$
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		AdaptExpression expression2 = new AdaptExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter"); //$NON-NLS-1$
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}
	
	public void testAdaptExpressionFail() throws Exception {
		AdaptExpression expression= new AdaptExpression("org.eclipse.core.internal.expressions.tests.NotExisting"); //$NON-NLS-1$
		EvaluationResult result= expression.evaluate(new EvaluationContext(null, new Adaptee()));
		assertTrue(result == EvaluationResult.FALSE);
	}
	
	public void testAdaptExpressionFail2() throws Exception {
		AdaptExpression expression= new AdaptExpression("org.eclipse.core.internal.expressions.tests.Adapter"); //$NON-NLS-1$
		expression.add(new InstanceofExpression("org.eclipse.core.internal.expressions.tests.NotExisting")); //$NON-NLS-1$
		EvaluationResult result= expression.evaluate(new EvaluationContext(null, new Adaptee()));
		assertTrue(result == EvaluationResult.FALSE);
	}

	public void testAndExpressionNotEqual() throws Exception {
		AndExpression expression1 = new AndExpression();
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter1")); //$NON-NLS-1$
		AndExpression expression2 = new AndExpression();
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter2")); //$NON-NLS-1$
		assertTrue("These and expressions should not be equal", !expression1
				.equals(expression2));
	}

	public void testAndExpressionHashCode() throws Exception {
		AndExpression expression1 = new AndExpression();
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		AndExpression expression2 = new AndExpression();
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testCountExpressionNotEqual() throws Exception {
		CountExpression expression1 = new CountExpression("+");
		CountExpression expression2 = new CountExpression("!");
		assertTrue("These count expressions should not be equal", !expression1
				.equals(expression2));
	}

	public void testCountExpressionHashCode() throws Exception {
		CountExpression expression1 = new CountExpression("*");
		CountExpression expression2 = new CountExpression("*");
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testEnablementExpressionNotEqual() throws Exception {
		EnablementExpression expression1 = new EnablementExpression((IConfigurationElement)null);
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter1")); //$NON-NLS-1$
		EnablementExpression expression2 = new EnablementExpression((IConfigurationElement)null);
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter2")); //$NON-NLS-1$
		assertTrue("These enablement expressions should not be equal",
				!expression1.equals(expression2));
	}

	public void testEnablementExpressionHashCode() throws Exception {
		EnablementExpression expression1 = new EnablementExpression((IConfigurationElement)null);
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		EnablementExpression expression2 = new EnablementExpression((IConfigurationElement)null);
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testEqualsExpressionNotEqual() throws Exception {
		EqualsExpression expression1 = new EqualsExpression("+");
		EqualsExpression expression2 = new EqualsExpression("!");
		assertTrue("These equals expressions should not be equal", !expression1
				.equals(expression2));
	}

	public void testEqualsExpressionHashCode() throws Exception {
		EqualsExpression expression1 = new EqualsExpression("*");
		EqualsExpression expression2 = new EqualsExpression("*");
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testInstanceOfExpressionNotEqual() throws Exception {
		InstanceofExpression expression1 = new InstanceofExpression("+");
		InstanceofExpression expression2 = new InstanceofExpression("!");
		assertTrue("These instance of expressions should not be equal",
				!expression1.equals(expression2));
	}

	public void testInstanceOfExpressionHashCode() throws Exception {
		InstanceofExpression expression1 = new InstanceofExpression("*");
		InstanceofExpression expression2 = new InstanceofExpression("*");
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testIterateExpressionNotEqual() throws Exception {
		IterateExpression expression1 = new IterateExpression("or");
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		IterateExpression expression2 = new IterateExpression("and");
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		assertTrue("These iterate expressions should not be equal",
				!expression1.equals(expression2));
	}

	public void testIterateExpressionHashCode() throws Exception {
		IterateExpression expression1 = new IterateExpression("and");
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		IterateExpression expression2 = new IterateExpression("and");
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testNotExpressionNotEqual() throws Exception {
		NotExpression expression1 = new NotExpression(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter1"));
		NotExpression expression2 = new NotExpression(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter2"));
		assertTrue("These not expressions should not be equal", !expression1
				.equals(expression2));
	}

	public void testNotExpressionHashCode() throws Exception {
		NotExpression expression1 = new NotExpression(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter"));
		NotExpression expression2 = new NotExpression(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter"));
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testOrExpressionNotEqual() throws Exception {
		OrExpression expression1 = new OrExpression();
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter1")); //$NON-NLS-1$
		OrExpression expression2 = new OrExpression();
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter2")); //$NON-NLS-1$
		assertTrue("These or expressions should not be equal", !expression1
				.equals(expression2));
	}

	public void testOrExpressionHashCode() throws Exception {
		OrExpression expression1 = new OrExpression();
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		OrExpression expression2 = new OrExpression();
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testResolveExpressionNotEqual() throws Exception {
		ResolveExpression expression1 = new ResolveExpression("variable1",
				new Object[0]);
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter1")); //$NON-NLS-1$
		ResolveExpression expression2 = new ResolveExpression("variable2",
				new Object[0]);
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter2")); //$NON-NLS-1$
		assertTrue("These resolve expressions should not be equal",
				!expression1.equals(expression2));
	}

	public void testResolveExpressionHashCode() throws Exception {
		ResolveExpression expression1 = new ResolveExpression("variable",
				new Object[0]);
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		ResolveExpression expression2 = new ResolveExpression("variable",
				new Object[0]);
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testSystemTestExpressionNotEqual() throws Exception {
		SystemTestExpression expression1 = new SystemTestExpression("prop",
				"value1");
		SystemTestExpression expression2 = new SystemTestExpression("prop",
				"value2");
		assertTrue("These system test expressions should not be equal",
				!expression1.equals(expression2));
	}

	public void testSystemTestExpressionHashCode() throws Exception {
		SystemTestExpression expression1 = new SystemTestExpression("prop",
				"value");
		SystemTestExpression expression2 = new SystemTestExpression("prop",
				"value");
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testTestExpressionNotEqual() throws Exception {
		TestExpression expression1 = new TestExpression("namespace", "prop",
				new Object[0], "value1");
		TestExpression expression2 = new TestExpression("namespace", "prop",
				new Object[0], "value2");
		assertTrue("These system test expressions should not be equal",
				!expression1.equals(expression2));
	}

	public void testTestExpressionHashCode() throws Exception {
		TestExpression expression1 = new TestExpression("namespace", "prop",
				new Object[0], "value");
		TestExpression expression2 = new TestExpression("namespace", "prop",
				new Object[0], "value");
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}

	public void testWithExpressionNotEqual() throws Exception {
		WithExpression expression1 = new WithExpression("variable1");
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter1")); //$NON-NLS-1$
		WithExpression expression2 = new WithExpression("variable2");
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter2")); //$NON-NLS-1$
		assertTrue("These with expressions should not be equal",
				!expression1.equals(expression2));
	}

	public void testWithExpressionHashCode() throws Exception {
		WithExpression expression1 = new WithExpression("variable");
		expression1.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		WithExpression expression2 = new WithExpression("variable");
		expression2.add(new InstanceofExpression(
				"org.eclipse.core.internal.expressions.tests.Adapter")); //$NON-NLS-1$
		assertEquals("Equal expressions should have the same hash code",
				expression1.hashCode(), expression2.hashCode());
	}
	
	public void testVariableResolver() throws Exception {
		final Object result= new Object();
		IVariableResolver resolver= new IVariableResolver() {
			public Object resolve(String name, Object[] args) throws CoreException {
				assertEquals("variable", name); //$NON-NLS-1$
				assertEquals("arg1", args[0]); //$NON-NLS-1$
				assertEquals(Boolean.TRUE, args[1]);
				return result;
			}
		};
		EvaluationContext context= new EvaluationContext(null, new Object(), new IVariableResolver[] { resolver });
		assertTrue(result == context.resolveVariable("variable", new Object[] {"arg1", Boolean.TRUE})); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void testEqualsExpression() throws Exception {
		EqualsExpression exp= new EqualsExpression("name"); //$NON-NLS-1$
		EvaluationContext context= new EvaluationContext(null, "name"); //$NON-NLS-1$
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		
		exp= new EqualsExpression(Boolean.TRUE);
		context= new EvaluationContext(null, Boolean.TRUE);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));		
		
		exp= new EqualsExpression("name"); //$NON-NLS-1$
		context= new EvaluationContext(null, Boolean.TRUE);
		assertTrue(EvaluationResult.FALSE == exp.evaluate(context));		
	}
	
	public void testCountExpressionAnyNumber() throws Exception {
		CountExpression exp= new CountExpression("*"); //$NON-NLS-1$
		
		List list= new ArrayList();
		EvaluationContext context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		
		list.clear();
		list.add("one"); //$NON-NLS-1$
		context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		
		list.clear();
		list.add("one"); //$NON-NLS-1$
		list.add("two"); //$NON-NLS-1$
		list.add("three"); //$NON-NLS-1$
		context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
	}
	
	public void testCountExpressionExact() throws Exception {
		CountExpression exp= new CountExpression("2"); //$NON-NLS-1$
		
		List list= new ArrayList();
		list.add("one"); //$NON-NLS-1$
		EvaluationContext context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.FALSE == exp.evaluate(context));
		
		list.clear();
		list.add("one"); //$NON-NLS-1$
		list.add("two"); //$NON-NLS-1$
		context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		
		list.clear();
		list.add("one"); //$NON-NLS-1$
		list.add("two"); //$NON-NLS-1$
		list.add("three"); //$NON-NLS-1$
		context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.FALSE == exp.evaluate(context));
	}
	
	public void testCountExpressionNoneOrOne() throws Exception {
		CountExpression exp= new CountExpression("?"); //$NON-NLS-1$
		
		List list= new ArrayList();
		EvaluationContext context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		
		list.clear();
		list.add("one"); //$NON-NLS-1$
		context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		
		list.clear();
		list.add("one"); //$NON-NLS-1$
		list.add("two"); //$NON-NLS-1$
		context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.FALSE == exp.evaluate(context));
	}
	
	public void testCountExpressionOneOrMore() throws Exception {
		CountExpression exp= new CountExpression("+"); //$NON-NLS-1$
		
		List list= new ArrayList();
		EvaluationContext context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.FALSE == exp.evaluate(context));
		
		list.clear();
		list.add("one"); //$NON-NLS-1$
		context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		
		list.clear();
		list.add("one"); //$NON-NLS-1$
		list.add("two"); //$NON-NLS-1$
		context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
	}
	
	public void testCountExpressionNone() throws Exception {
		CountExpression exp= new CountExpression("!"); //$NON-NLS-1$
		
		List list= new ArrayList();
		EvaluationContext context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		
		list.clear();
		list.add("one"); //$NON-NLS-1$
		context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.FALSE == exp.evaluate(context));
		
		list.clear();
		list.add("one"); //$NON-NLS-1$
		list.add("two"); //$NON-NLS-1$
		context= new EvaluationContext(null, list);
		assertTrue(EvaluationResult.FALSE == exp.evaluate(context));
	}
	
	public void testInstanceofTrue() throws Exception {
		B b= new B();
		EvaluationContext context= new EvaluationContext(null, b);
		
		InstanceofExpression exp= new InstanceofExpression("org.eclipse.core.internal.expressions.tests.B"); //$NON-NLS-1$
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));

		exp= new InstanceofExpression("org.eclipse.core.internal.expressions.tests.A"); //$NON-NLS-1$
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		
		exp= new InstanceofExpression("org.eclipse.core.internal.expressions.tests.I"); //$NON-NLS-1$
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
	}
	
	public void testInstanceofFalse() throws Exception {
		A a= new A();
		EvaluationContext context= new EvaluationContext(null, a);
		
		InstanceofExpression exp= new InstanceofExpression("org.eclipse.core.internal.expressions.tests.B"); //$NON-NLS-1$
		assertTrue(EvaluationResult.FALSE == exp.evaluate(context));
	}
	
	public void testIterateExpressionAndTrue() throws Exception {
		final List result= new ArrayList();
		Expression myExpression= new Expression() {
			public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
				result.add(context.getDefaultVariable());
				return EvaluationResult.TRUE;
			}
		};
		IterateExpression exp= new IterateExpression("and"); //$NON-NLS-1$
		exp.add(myExpression);
		List input= new ArrayList();
		input.add("one"); //$NON-NLS-1$
		input.add("two"); //$NON-NLS-1$
		EvaluationContext context= new EvaluationContext(null, input);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		assertTrue(result.equals(input));
	}
	
	public void testIterateExpressionAndFalse() throws Exception {
		final List result= new ArrayList();
		Expression myExpression= new Expression() {
			public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
				result.add(context.getDefaultVariable());
				return EvaluationResult.FALSE;
			}
		};
		IterateExpression exp= new IterateExpression("and"); //$NON-NLS-1$
		exp.add(myExpression);
		List input= new ArrayList();
		input.add("one"); //$NON-NLS-1$
		input.add("two"); //$NON-NLS-1$
		EvaluationContext context= new EvaluationContext(null, input);
		assertTrue(EvaluationResult.FALSE == exp.evaluate(context));
		assertTrue(result.size() == 1 && result.get(0).equals("one")); //$NON-NLS-1$
	}
	
	public void testIterateExpressionOrTrue() throws Exception {
		final List result= new ArrayList();
		Expression myExpression= new Expression() {
			public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
				result.add(context.getDefaultVariable());
				return EvaluationResult.TRUE;
			}
		};
		IterateExpression exp= new IterateExpression("or"); //$NON-NLS-1$
		exp.add(myExpression);
		List input= new ArrayList();
		input.add("one"); //$NON-NLS-1$
		input.add("two"); //$NON-NLS-1$
		EvaluationContext context= new EvaluationContext(null, input);
		assertTrue(EvaluationResult.TRUE == exp.evaluate(context));
		assertTrue(result.size() == 1 && result.get(0).equals("one")); //$NON-NLS-1$
	}
	
	public void testIterateExpressionOrFalse() throws Exception {
		final List result= new ArrayList();
		Expression myExpression= new Expression() {
			public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
				result.add(context.getDefaultVariable());
				return EvaluationResult.FALSE;
			}
		};
		IterateExpression exp= new IterateExpression("or"); //$NON-NLS-1$
		exp.add(myExpression);
		List input= new ArrayList();
		input.add("one"); //$NON-NLS-1$
		input.add("two"); //$NON-NLS-1$
		EvaluationContext context= new EvaluationContext(null, input);
		assertTrue(EvaluationResult.FALSE == exp.evaluate(context));
		assertTrue(result.equals(input));
	}
	
	public void testReadXMLExpression() throws Exception {
		IExtensionRegistry registry= Platform.getExtensionRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor("org.eclipse.core.expressions.tests", "testParticipants"); //$NON-NLS-1$ //$NON-NLS-2$
		
		IConfigurationElement enable= findExtension(ces, "test1").getChildren("enablement")[0]; //$NON-NLS-1$ //$NON-NLS-2$
		Expression exp= ExpressionConverter.getDefault().perform(enable);
		ref(exp);
	}
	
	public void testForcePluginActivation() throws Exception {
		IExtensionRegistry registry= Platform.getExtensionRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor("org.eclipse.core.expressions.tests", "testParticipants"); //$NON-NLS-1$ //$NON-NLS-2$
		
		IConfigurationElement enable= findExtension(ces, "test2").getChildren("enablement")[0]; //$NON-NLS-1$ //$NON-NLS-2$
		EnablementExpression exp= (EnablementExpression) ExpressionConverter.getDefault().perform(enable);
		Expression[] children= exp.getChildren();
		assertTrue(children.length == 3);
		TestExpression test= (TestExpression) children[0];
		assertTrue(test.testGetForcePluginActivation());
		test= (TestExpression) children[1];
		assertTrue(!test.testGetForcePluginActivation());
		test= (TestExpression) children[2];
		assertTrue(!test.testGetForcePluginActivation());
	}

	public void testPlatformPropertyTester() throws Exception {
		IExtensionRegistry registry= Platform.getExtensionRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor("org.eclipse.core.expressions.tests", "testParticipants"); //$NON-NLS-1$ //$NON-NLS-2$
		
		IConfigurationElement enable= findExtension(ces, "test3").getChildren("enablement")[0]; //$NON-NLS-1$ //$NON-NLS-2$
		Expression exp= ExpressionConverter.getDefault().perform(enable);
		EvaluationContext context = new EvaluationContext(null, Platform.class);
		assertEquals(EvaluationResult.TRUE, exp.evaluate(context));
	}

	private IConfigurationElement findExtension(IConfigurationElement[] ces, String id) {
		for (int i= 0; i < ces.length; i++) {
			if (id.equals(ces[i].getAttribute("id"))) //$NON-NLS-1$
				return ces[i];
		}
		return null;
	}
	
	protected void ref(Expression exp) {
	}
}
