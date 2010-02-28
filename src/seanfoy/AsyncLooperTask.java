package seanfoy;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * AsyncLooperTask enables proper and easy use of the UI thread in
 * combination with a Looper. This class allows to perform
 * background operations and publish results on the UI thread
 * without having to manipulate threads and/or handlers.
 *
 * An asynchronous task is defined by a computation that runs on a
 * background thread and whose result is published on the UI
 * thread. An asynchronous task is defined by 3 generic types,
 * called Params, Progress and Result, and 4 steps, called begin,
 * doInBackground, processProgress and end.
 *
 * @param <Params> the type of the parameters sent to the task upon execution.
 * @param <Progress> the type of the progress units published during the
 * background computation.
 * @param <Result> the type of the result of the background computation.
 * 
 * @see AsyncTask
 */
public abstract class AsyncLooperTask<Params, Progress, Result> {
    /**
     * Creates a new asynchronous task associated with the
     * current (presumably UI) thread.
     */
    public AsyncLooperTask() {
        callerHandler = new Handler();
    }
    /**
     * Executes the task with the specified parameters
     * @param P The parameters of the task.
     * @return This instance of AsyncTask.
     */
    public AsyncLooperTask<Params, Progress, Result> execute(final Params... P) {
        new HandlerThread(getClass().getName()) {
            @Override
            public void run() {
                onPreExecute();
                final Result r = doInBackground(P);
                callerHandler.post(
                    new Runnable() {
                        public void run() {
                            onPostExecute(r);
                        }
                    });
            }
        }.start();
        return this;
    }
    /**
     * Runs on the UI thread before doInBackground(Params...).
     */
    public void onPreExecute() {}
    /**
     * Override this method to perform a computation on a background thread.
     * @param P The parameters of the task.
     * @return A result, defined by the subclass of this task.
     */
    abstract public Result doInBackground(Params... P);
    /**
     * Runs on the UI thread after doInBackground(Params...).
     * @param r The result of the operation computed by doInBackground(Params...).
     */
    public void onPostExecute(Result r) {}
    /**
     * This method can be invoked from doInBackground(Params...) to publish
     * updates on the UI thread while the background computation is still
     * running. Each call to this method will trigger the execution of
     * onProgressUpdate(Progress...)  on the UI thread.
     * @param progresses The progress values to update the UI with.
     */
    public void publishProgress(final Progress... progresses) {
        callerHandler.post(
            new Runnable() {
                public void run() {
                    onProgressUpdate(progresses);
                }
            });
    }
    /**
     * Runs on the UI thread after publishProgress(Progress...)  is invoked.
     * @param progresses The values indicating progress.
     */
    public void onProgressUpdate(Progress...progresses) {}
    Handler callerHandler;
}