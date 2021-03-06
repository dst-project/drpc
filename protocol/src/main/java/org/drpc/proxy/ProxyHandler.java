package org.drpc.proxy;

import org.drpc.Invoker;
import org.drpc.api.AsyncResponse;
import org.drpc.api.ProtobufRequestDelegate;
import org.drpc.api.Request;
import org.drpc.api.Response;
import org.drpc.exception.DrpcException;
import org.drpc.utils.RequestIdGenerator;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;


public class ProxyHandler<T> implements InvocationHandler {

  private Invoker invoker;

  private Class<T> interfaceClazz;

  private byte[] sessionId;

  public ProxyHandler(Class<T> clazz, Invoker invoker, byte[] sessionId) {
    interfaceClazz = clazz;
    this.invoker = invoker;
    this.sessionId = sessionId;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    if (isLocalMethod(method)) {
      throw new DrpcException("Can not invoke local method: " + method.getName());
    }

    Request request = new ProtobufRequestDelegate();
    request.setRequestId(RequestIdGenerator.next());
    request.setInterfaceName(method.getDeclaringClass().getName());
    request.setMethodName(method.getName());
    request.setArgsValue(args);
    // Do not keep order when sessionID is null
    if (sessionId != null) {
      request.setSessionID(sessionId);
    }
    request.build();

    Class<?> returnType = method.getReturnType();
    Response response = invoker.invoke(request);
    if (returnType == Void.TYPE) {
      return null;
    }

    // async-method
    if (CompletableFuture.class.isAssignableFrom(returnType)) {
      CompletableFuture future = new CompletableFuture();
      AsyncResponse asyncResponse = (AsyncResponse) response;
      asyncResponse.whenComplete((v, t) -> {
        if (t != null) {
          future.completeExceptionally(t);
        } else {
          if (v.isError()) {
            future.completeExceptionally(v.getThrowable());
          } else {
            try {
              Type returnGenericType = method.getGenericReturnType();
              future.complete(resolveReturnValue(v.getValue(), returnGenericType));
            } catch (Exception e) {
              future.completeExceptionally(e);
            }
          }
        }
      });
      return future;
    }

    // sync-method
    if (response.getThrowable() != null) {
      throw response.getThrowable();
    }
    return resolveReturnValue(response.getValue(), returnType);
  }

  @SuppressWarnings("unchecked")
  private Object resolveReturnValue(Object returnValue, Type resolvedReturnType)
      throws IOException {
    if (returnValue instanceof Any && resolvedReturnType instanceof ParameterizedType) {
      return ((Any) returnValue).unpack(
          (Class<? extends Message>) ((ParameterizedType) resolvedReturnType)
              .getActualTypeArguments()[0]);
    } else {
      return returnValue;
    }
  }

  private boolean isLocalMethod(Method method) {
    if (method.getDeclaringClass().equals(Object.class)) {
      try {
        interfaceClazz
            .getDeclaredMethod(method.getName(), method.getParameterTypes());
        return false;
      } catch (NoSuchMethodException e) {
        return true;
      }
    }
    return false;
  }
}
