package org.apache.maven.plugin.surefire.booterclient.output;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.surefire.booterclient.MockReporter;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.NotifiableTestStream;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.maven.plugin.surefire.booterclient.MockReporter.STDERR;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.STDOUT;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test for {@link ForkClient}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class ForkClientTest
{
    @Test
    public void shouldNotFailOnEmptyInput()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, null, logger, printedErrorStream, 0 );
        client.consumeLine( null );
        client.consumeLine( "   " );
        client.consumeMultiLineContent( null );
        client.consumeMultiLineContent( "   " );
    }

    @Test
    public void shouldAcquireNextTest()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:next-test\n" );
        verify( notifiableTestStream, times( 1 ) )
                .provideNewTest();
        verifyNoMoreInteractions( notifiableTestStream );
        verifyZeroInteractions( factory );
    }

    @Test
    public void shouldNotifyWithBye()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:bye\n" );
        verify( notifiableTestStream, times( 1 ) )
                .acknowledgeByeEventReceived();
        verifyNoMoreInteractions( notifiableTestStream );
        verifyZeroInteractions( factory );
        assertThat( client.isSaidGoodBye() )
                .isTrue();
    }

    @Test
    public void shouldStopOnNextTest()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        final boolean[] verified = {false};
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 )
        {
            @Override
            protected void stopOnNextTest()
            {
                super.stopOnNextTest();
                verified[0] = true;
            }
        };
        client.consumeMultiLineContent( ":maven:surefire:std:out:stop-on-next-test\n" );
        verifyZeroInteractions( notifiableTestStream );
        verifyZeroInteractions( factory );
        assertThat( verified[0] )
                .isTrue();
    }

    @Test
    public void shouldReceiveStdOut()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:std-out-stream:normal-run:UTF-8:bXNn\n" );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( STDOUT );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "msg" );
    }

    @Test
    public void shouldReceiveStdOutNewLine()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:std-out-stream-new-line:normal-run:UTF-8:bXNn\n" );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( STDOUT );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "msg\n" );
    }

    @Test
    public void shouldReceiveStdErr()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:std-err-stream:normal-run:UTF-8:bXNn\n" );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( STDERR );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "msg" );
    }

    @Test
    public void shouldReceiveStdErrNewLine()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:std-err-stream-new-line:normal-run:UTF-8:bXNn\n" );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( STDERR );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "msg\n" );
    }

}
