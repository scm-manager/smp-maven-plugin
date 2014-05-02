/**
 * Copyright (c) 2014, Sebastian Sdorra
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 'AS IS'
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * https://bitbucket.org/sdorra/smp-maven-plugin
 */


var gulp = require('gulp');
var uglify = require('gulp-uglify');
var concat = require('gulp-concat');
var rev = require('gulp-rev');
var filesize = require('gulp-filesize');
var ngmin = require('gulp-ngmin');
var minifycss = require('gulp-minify-css');
var less = require('gulp-less');

gulp.task('javascript', function(){
  
  gulp.src('${webappDirectory}/scripts/**/*.js')
    .pipe(concat('${artifactId}.js'))
    .pipe(filesize())
    .pipe(ngmin())
    .pipe(uglify())
    .pipe(rev())
    .pipe(filesize())
    .pipe(gulp.dest('${webappDirectory}/scripts/'));  
  
});

gulp.task('less', function(){
  
  gulp.src('${webappDirectory}/styles/main.less')
    .pipe(less())
    .pipe(minifycss())
    .pipe(rev())
    .pipe(filesize())
    .pipe(gulp.dest('${webappDirectory}/styles/'));
    
});

gulp.task('css', function(){
  
  gulp.src('${webappDirectory}/styles/**/*.css')
    .pipe(concat('${artifactId}.css'))
    .pipe(filesize())
    .pipe(minifycss())
    .pipe(rev())
    .pipe(filesize())
    .pipe(gulp.dest('${webappDirectory}/styles/'));  
  
});

gulp.task('default', ['javascript', 'css', 'less']);

gulp.on('err', function (err) {
  throw err;
});
