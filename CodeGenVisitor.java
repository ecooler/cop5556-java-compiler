package cop5556sp17;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import cop5556sp17.AST.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.Type.TypeName;

import static cop5556sp17.AST.Type.TypeName.*;
import static cop5556sp17.Scanner.Kind.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 */
	public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
	}

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;

	MethodVisitor mv; // visitor of method currently under construction


	SymbolTable decSymtab = new SymbolTable();
	Stack<Integer> slotStack = new Stack<>();
	private int slotNum;

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		className = program.getName();
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { "java/lang/Runnable" });
		cw.visitSource(sourceFileName, null);

		// generate constructor code
		// get a MethodVisitor
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null,
				null);
		mv.visitCode();
		// Create label at start of code
		Label constructorStart = new Label();
		mv.visitLabel(constructorStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering <init>");
		// generate code to call superclass constructor
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		// visit parameter decs to add each as field to the class
		// pass in mv so decs can add their initialization code to the
		// constructor.
		ArrayList<ParamDec> params = program.getParams();
		for (ParamDec dec : params)
			dec.visit(this, mv);
		mv.visitInsn(RETURN);
		// create label at end of code
		Label constructorEnd = new Label();
		mv.visitLabel(constructorEnd);
		// finish up by visiting local vars of constructor
		// the fourth and fifth arguments are the region of code where the local
		// variable is defined as represented by the labels we inserted.
		mv.visitLocalVariable("this", classDesc, null, constructorStart, constructorEnd, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, constructorStart, constructorEnd, 1);
		// indicates the max stack size for the method.
		// because we used the COMPUTE_FRAMES parameter in the classwriter
		// constructor, asm
		// will do this for us. The parameters to visitMaxs don't matter, but
		// the method must
		// be called.
		mv.visitMaxs(1, 1);
		// finish up code generation for this method.
		mv.visitEnd();
		// end of constructor

		// create main method which does the following
		// 1. instantiate an instance of the class being generated, passing the
		// String[] with command line arguments
		// 2. invoke the run method.
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null,
				null);
		mv.visitCode();
		Label mainStart = new Label();
		mv.visitLabel(mainStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering main");
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "([Ljava/lang/String;)V", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V", false);
		mv.visitInsn(RETURN);
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		mv.visitLocalVariable("instance", classDesc, null, mainStart, mainEnd, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		// create run method
		mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		mv.visitCode();
		Label startRun = new Label();
		mv.visitLabel(startRun);
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering run");
		program.getB().visit(this, null);
		mv.visitInsn(RETURN);
		Label endRun = new Label();
		mv.visitLabel(endRun);
		mv.visitLocalVariable("this", classDesc, null, startRun, endRun, 0);
//TODO  visit the local variables
        for (ParamDec paramDec : program.getParams()) {
            paramDec.visit(this, arg);
        }
        program.getB().visit(this, arg);
		mv.visitMaxs(1, 1);
		mv.visitEnd(); // end of run method
		
		
		cw.visitEnd();//end of class
		
		//generate classfile and return it
		return cw.toByteArray();
	}



	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		assignStatement.getE().visit(this, arg);
		CodeGenUtils.genPrint(DEVEL, mv, "\nassignment: " + assignStatement.var.getText() + "=");
		CodeGenUtils.genPrintTOS(GRADE, mv, assignStatement.getE().getTypeName());
		assignStatement.getVar().visit(this, arg);
		return null;
	}

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
		assert false : "not yet implemented";
		return null;
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
      //TODO  Implement this
		Token op = binaryExpression.getOp();
		switch (op.kind) {
			case PLUS:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IADD);
				break;
			case MINUS:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(ISUB);
				break;
			case TIMES:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IMUL);
				break;
			case DIV:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IDIV);
				break;
			case MOD:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IREM);
				break;
			case OR:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IREM);
				break;
			case AND:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IREM);
				break;
			case LT:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IF_ICMPLT);
				break;
			case LE:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IF_ICMPLE);
				break;
			case GT:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IF_ICMPGT);
				break;
			case GE:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IF_ICMPGE);
				break;
			case EQUAL:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IF_ACMPEQ);
				break;
			case NOTEQUAL:
				binaryExpression.getE0().visit(this, arg);
				binaryExpression.getE1().visit(this, arg);
				mv.visitInsn(IF_ACMPNE);
				break;
			default:
				throw new RuntimeException("not yet implemented");
		}
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		//TODO  Implement this
		Label startBlock = new Label();
		Label endBlock = new Label();
		mv.visitLabel(startBlock);
		decSymtab.enterScope();
		for (Dec dec : block.getDecs()) {
			dec.visit(this, arg);
		}
		for (Statement statement : block.getStatements()) {
			statement.visit(this, arg);
		}
		mv.visitLabel(endBlock);
		decSymtab.leaveScope();
		for (Dec dec : block.getDecs()) {
			dec.visit(this, arg);
			mv.visitLocalVariable(dec.getIdent().getText(), dec.getTypeName().getJVMTypeDesc(), null, startBlock, endBlock, dec.getSlotNumber());
			CodeGenUtils.genPrint(GRADE, mv, "\ndec#" + dec.getSlotNumber() + " is " + dec.getIdent().getText());
		}
		for (int i = 0; i<block.getDecs().size(); i++){
			slotStack.pop();
		}
		return null;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		//TODO Implement this
		if (booleanLitExpression.getValue()) {
			mv.visitInsn(ICONST_1);
		} else {
			mv.visitInsn(ICONST_0);
		}
		return null;
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
		assert false : "not yet implemented";
		return null;
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		//TODO Implement this
		mv.visitVarInsn(ALOAD, slotNum);
		slotStack.push(slotNum++);
		mv.visitVarInsn(BIPUSH, declaration.getSlotNumber());
		mv.visitInsn(ALOAD);
		if (declaration.getTypeName().isType(TypeName.INTEGER)) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf","(I)Ljava/lang/Integer;", true);
		} else if (declaration.getTypeName().isType(BOOLEAN)) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf","(I)Ljava/lang/Boolean;", true);
		}
		mv.visitFieldInsn(PUTFIELD, className, declaration.getIdent().getText(), "Ljava/lang/String;");
		return null;
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
		assert false : "not yet implemented";
		return null;
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		assert false : "not yet implemented";
		return null;
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
		assert false : "not yet implemented";
		return null;
	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		//TODO Implement this
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		return null;
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		//TODO Implement this
		mv.visitVarInsn(ASTORE,slotNum);
		mv.visitFieldInsn(PUTFIELD, className, identX.getText(), "Ljava/lang/String;");
		return null;

	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		//TODO Implement this
		ifStatement.getE().visit(this, arg);
		Label AFTER = new Label();
		mv.visitJumpInsn(IFNE, AFTER);
		ifStatement.getB().visit(this, arg);
		mv.visitLabel(AFTER);
		return null;
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		assert false : "not yet implemented";
		return null;
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		//TODO Implement this
		mv.visitInsn(intLitExpression.value);
		return null;
	}


	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		//TODO Implement this
		//For assignment 5, only needs to handle integers and booleans
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitVarInsn(BIPUSH, paramDec.getSlotNumber());
		mv.visitInsn(ALOAD);
		if (paramDec.getTypeName().isType(TypeName.INTEGER)) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf","(I)Ljava/lang/Integer;", true);
		} else if (paramDec.getTypeName().isType(BOOLEAN)) {
		    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf","(I)Ljava/lang/Boolean;", true);
        }
        mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), "Ljava/lang/String;");
		return null;

	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		assert false : "not yet implemented";
		return null;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		assert false : "not yet implemented";
		return null;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		//TODO Implement this
		Label GUARD = new Label();
		Label BODY = new Label();
		mv.visitJumpInsn(GOTO, GUARD);
		mv.visitLabel(BODY);
		whileStatement.getB().visit(this, arg);
		mv.visitLabel(GUARD);
		whileStatement.getE().visit(this, arg);
		mv.visitJumpInsn(IFNE, BODY);
		return null;
	}

}
