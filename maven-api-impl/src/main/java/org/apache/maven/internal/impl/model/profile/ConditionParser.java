/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.internal.impl.model.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConditionParser {

    public interface ExpressionFunction {
        Object apply(List<Object> args);
    }

    private final Map<String, ExpressionFunction> functions;
    private List<String> tokens;
    private int current;

    public ConditionParser(Map<String, ExpressionFunction> functions) {
        this.functions = functions;
    }

    public Object parse(String expression) {
        this.tokens = tokenize(expression);
        this.current = 0;
        return parseExpression();
    }

    private List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        char quoteType = 0;
        boolean inPropertyAlias = false;
        int propertyAliasDepth = 0;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (quoteType != 0) {
                // Inside quotes, don't process aliases
                if (c == quoteType) {
                    quoteType = 0;
                    sb.append(c);
                    tokens.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
                continue;
            }

            if (inPropertyAlias) {
                if (c == '}') {
                    propertyAliasDepth--;
                    if (propertyAliasDepth == 0) {
                        inPropertyAlias = false;
                        tokens.add("property");
                        tokens.add("(");
                        tokens.add("'" + sb.toString() + "'");
                        tokens.add(")");
                        sb.setLength(0);
                    } else {
                        sb.append(c);
                    }
                } else {
                    if (c == '$' && i + 1 < expression.length() && expression.charAt(i + 1) == '{') {
                        propertyAliasDepth++;
                        i++; // Skip the '{'
                    }
                    sb.append(c);
                }
                continue;
            }

            if (c == '$' && i + 1 < expression.length() && expression.charAt(i + 1) == '{') {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                inPropertyAlias = true;
                propertyAliasDepth = 1;
                i++; // Skip the '{'
                continue;
            }

            if (c == '"' || c == '\'') {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                quoteType = c;
                sb.append(c);
            } else if (c == ' ' || c == '(' || c == ')' || c == ',' || c == '+' || c == '>' || c == '<' || c == '='
                    || c == '!') {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                if (c != ' ') {
                    if ((c == '>' || c == '<' || c == '=' || c == '!')
                            && i + 1 < expression.length()
                            && expression.charAt(i + 1) == '=') {
                        tokens.add(c + "=");
                        i++; // Skip the next character
                    } else {
                        tokens.add(String.valueOf(c));
                    }
                }
            } else {
                sb.append(c);
            }
        }

        if (inPropertyAlias) {
            throw new RuntimeException("Unclosed property alias: ${");
        }

        if (!sb.isEmpty()) {
            tokens.add(sb.toString());
        }

        return tokens;
    }

    private Object parseExpression() {
        Object result = parseLogicalOr();
        if (current < tokens.size()) {
            throw new RuntimeException("Unexpected tokens after end of expression");
        }
        return result;
    }

    private Object parseLogicalOr() {
        Object left = parseLogicalAnd();
        while (current < tokens.size() && tokens.get(current).equals("||")) {
            current++;
            Object right = parseLogicalAnd();
            left = (boolean) left || (boolean) right;
        }
        return left;
    }

    private Object parseLogicalAnd() {
        Object left = parseComparison();
        while (current < tokens.size() && tokens.get(current).equals("&&")) {
            current++;
            Object right = parseComparison();
            left = (boolean) left && (boolean) right;
        }
        return left;
    }

    private Object parseComparison() {
        Object left = parseAddSubtract();
        while (current < tokens.size()
                && (tokens.get(current).equals(">")
                        || tokens.get(current).equals("<")
                        || tokens.get(current).equals(">=")
                        || tokens.get(current).equals("<=")
                        || tokens.get(current).equals("==")
                        || tokens.get(current).equals("!="))) {
            String operator = tokens.get(current);
            current++;
            Object right = parseAddSubtract();
            left = compare(left, operator, right);
        }
        return left;
    }

    private Object parseAddSubtract() {
        Object left = parseMultiplyDivide();
        while (current < tokens.size()
                && (tokens.get(current).equals("+") || tokens.get(current).equals("-"))) {
            String operator = tokens.get(current);
            current++;
            Object right = parseMultiplyDivide();
            if (operator.equals("+")) {
                left = add(left, right);
            } else {
                left = subtract(left, right);
            }
        }
        return left;
    }

    private Object parseMultiplyDivide() {
        Object left = parseUnary();
        while (current < tokens.size()
                && (tokens.get(current).equals("*") || tokens.get(current).equals("/"))) {
            String operator = tokens.get(current);
            current++;
            Object right = parseUnary();
            if (operator.equals("*")) {
                left = multiply(left, right);
            } else {
                left = divide(left, right);
            }
        }
        return left;
    }

    private Object parseUnary() {
        if (current < tokens.size() && tokens.get(current).equals("-")) {
            current++;
            Object value = parseUnary();
            return negate(value);
        }
        return parseTerm();
    }

    private Object parseTerm() {
        if (current >= tokens.size()) {
            throw new RuntimeException("Unexpected end of expression");
        }

        String token = tokens.get(current);
        if (token.equals("(")) {
            return parseParentheses();
        } else if (functions.containsKey(token)) {
            return parseFunction();
        } else if ((token.startsWith("\"") && token.endsWith("\"")) || (token.startsWith("'") && token.endsWith("'"))) {
            current++;
            return token.length() > 1 ? token.substring(1, token.length() - 1) : "";
        } else if (token.equalsIgnoreCase("true") || token.equalsIgnoreCase("false")) {
            current++;
            return Boolean.parseBoolean(token);
        } else {
            try {
                current++;
                return Double.parseDouble(token);
            } catch (NumberFormatException e) {
                // If it's not a number, treat it as a variable or unknown function
                return parseVariableOrUnknownFunction();
            }
        }
    }

    private Object parseVariableOrUnknownFunction() {
        current--; // Move back to the token we couldn't parse as a number
        String name = tokens.get(current);
        current++;

        // Check if it's followed by an opening parenthesis, indicating a function call
        if (current < tokens.size() && tokens.get(current).equals("(")) {
            // It's a function call, parse it as such
            List<Object> args = parseArgumentList();
            if (functions.containsKey(name)) {
                return functions.get(name).apply(args);
            } else {
                throw new RuntimeException("Unknown function: " + name);
            }
        } else {
            // It's a variable
            // Here you might want to handle variables differently
            // For now, we'll throw an exception
            throw new RuntimeException("Unknown variable: " + name);
        }
    }

    private List<Object> parseArgumentList() {
        List<Object> args = new ArrayList<>();
        current++; // Skip the opening parenthesis
        while (current < tokens.size() && !tokens.get(current).equals(")")) {
            args.add(parseLogicalOr());
            if (current < tokens.size() && tokens.get(current).equals(",")) {
                current++;
            }
        }
        if (current >= tokens.size() || !tokens.get(current).equals(")")) {
            throw new RuntimeException("Mismatched parentheses: missing closing parenthesis in function call");
        }
        current++; // Skip the closing parenthesis
        return args;
    }

    private Object parseFunction() {
        String functionName = tokens.get(current);
        current++;
        List<Object> args = parseArgumentList();
        return functions.get(functionName).apply(args);
    }

    private Object parseParentheses() {
        current++; // Skip the opening parenthesis
        Object result = parseLogicalOr();
        if (current >= tokens.size() || !tokens.get(current).equals(")")) {
            throw new RuntimeException("Mismatched parentheses: missing closing parenthesis");
        }
        current++; // Skip the closing parenthesis
        return result;
    }

    private static Object add(Object left, Object right) {
        if (left instanceof String || right instanceof String) {
            return toString(left) + toString(right);
        } else if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() + ((Number) right).doubleValue();
        } else {
            throw new RuntimeException("Cannot add " + left + " and " + right);
        }
    }

    private Object negate(Object value) {
        if (value instanceof Number) {
            return -((Number) value).doubleValue();
        }
        throw new RuntimeException("Cannot negate non-numeric value: " + value);
    }

    private static Object subtract(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() - ((Number) right).doubleValue();
        } else {
            throw new RuntimeException("Cannot subtract " + right + " from " + left);
        }
    }

    private static Object multiply(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() * ((Number) right).doubleValue();
        } else {
            throw new RuntimeException("Cannot multiply " + left + " and " + right);
        }
    }

    private static Object divide(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double divisor = ((Number) right).doubleValue();
            if (divisor == 0) {
                throw new ArithmeticException("Division by zero");
            }
            return ((Number) left).doubleValue() / divisor;
        } else {
            throw new RuntimeException("Cannot divide " + left + " by " + right);
        }
    }

    private static Object compare(Object left, String operator, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            if ("==".equals(operator)) {
                return false;
            } else if ("!=".equals(operator)) {
                return true;
            }
        }
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return switch (operator) {
                case ">" -> leftVal > rightVal;
                case "<" -> leftVal < rightVal;
                case ">=" -> leftVal >= rightVal;
                case "<=" -> leftVal <= rightVal;
                case "==" -> Math.abs(leftVal - rightVal) < 1e-9;
                case "!=" -> Math.abs(leftVal - rightVal) >= 1e-9;
                default -> throw new IllegalStateException("Unknown operator: " + operator);
            };
        } else if (left instanceof String && right instanceof String) {
            int comparison = ((String) left).compareTo((String) right);
            return switch (operator) {
                case ">" -> comparison > 0;
                case "<" -> comparison < 0;
                case ">=" -> comparison >= 0;
                case "<=" -> comparison <= 0;
                case "==" -> comparison == 0;
                case "!=" -> comparison != 0;
                default -> throw new IllegalStateException("Unknown operator: " + operator);
            };
        }
        throw new RuntimeException("Cannot compare " + left + " and " + right + " with operator " + operator);
    }

    public static String toString(Object value) {
        if (value instanceof Double) {
            double doubleValue = (Double) value;
            if (doubleValue == Math.floor(doubleValue)) {
                return String.format("%.0f", doubleValue);
            }
        }
        return String.valueOf(value);
    }

    public static Boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        } else if (value instanceof String s) {
            return !s.isBlank();
        } else if (value instanceof Number b) {
            return b.intValue() != 0;
        } else {
            return value != null;
        }
    }

    public static double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert string to number: " + value);
            }
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? 1.0 : 0.0;
        } else {
            throw new RuntimeException("Cannot convert to number: " + value);
        }
    }
}
