/*******************************************************************************
 * Copyright (c) 2015 Microsoft Research. All rights reserved. 
 *
 * The MIT License (MIT)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 ******************************************************************************/

package tlc2.value;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import tla2sany.semantic.FormalParamNode;
import tla2sany.semantic.SemanticNode;
import tlc2.tool.TLCState;
import tlc2.tool.Tool;
import tlc2.util.Context;
import util.UniqueString;

public class SerializationTest {

	@Test
	public void test() throws IOException {
		ModelValue.init();

		// Create a more or less complex Value graph
		final List<Value> values = new ArrayList<Value>();
//		values.add(getFcnLambda(100));
		values.add(getRecord(100));
		values.add(getFcnRecord(100));
		values.add(getTuple(100));
		final Value root = new TupleValue(values.toArray(new Value[values.size()]));

		// Finally collect all model values
		ModelValue.setValues();
		
		final File createTempFile = File.createTempFile("SerializationTest", ".bin");
		// write this hierarchy to a file  
		final ValueOutputStream out = new ValueOutputStream(createTempFile);
		final int roots = 90000000;
		for (int i = 0; i < roots; i++) {
			out.write(root);
		}
		out.close();
		
		// read in again
		final ValueInputStream in = new ValueInputStream(createTempFile);
		for (int i = 0; i < roots; i++) {
			final Value iv = in.read();
			Assert.assertEquals(root, iv);
		}
		in.close();
	}
//
//	private Value getFcnLambda(int i) {
//		final FormalParamNode[][] formals = new FormalParamNode[1][1];
//		formals[0][0] = EasyMock.createNiceMock(FormalParamNode.class);
//		final Value[] domains = new Value[1];
//		domains[0] = new IntervalValue(0, 1);
//		final FcnParams params = new FcnParams(formals, new boolean[1], domains);
//		
//		SemanticNode fbody = EasyMock.createNiceMock(SemanticNode.class);
//		TLCState s0 = EasyMock.createNiceMock(TLCState.class);
//		TLCState s1 = EasyMock.createNiceMock(TLCState.class);
//		Tool tool = EasyMock.createNiceMock(Tool.class);
//		FcnLambdaValue fcnLambdaValue = new FcnLambdaValue(params, fbody, tool, Context.BaseBranch, s0, s1, 4711);
//		fcnLambdaValue.fcnRcd = fcnLambdaValue.toFcnRcd();
//		return fcnLambdaValue;
//	}

	private Value getTuple(int num) {
		final Value[] values = new Value[num];
		for (int i = 0; i < num; i++) {
			values[i] = IntValue.gen(i);
		}
		return new TupleValue(values);
	}

	private Value getFcnRecord(int num) {
		final Value[] domains = new Value[num];
		final Value[] values = new Value[num];
		for (int i = 0; i < values.length; i++) {
			domains[i] = ModelValue.make("FcnRcdValueDomain"+i);
			values[i] = ModelValue.make("FcnRcdValueValue" + i);
		}
		return new FcnRcdValue(domains, values, false);
	}

	private Value getRecord(final int num) {
		final UniqueString[] rcdNames = new UniqueString[num];
		final Value[] rcdChildren = new Value[num];

		for (int i = 0; i < rcdChildren.length; i++) {
			rcdNames[i] = new StringValue("RecordValue"+i).getVal();
			rcdChildren[i] = ModelValue.make(rcdNames[i].toString());
		}
		return new RecordValue(rcdNames, rcdChildren, false);
	}
}