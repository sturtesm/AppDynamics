{
  'targets': [
    {
      'target_name': 'zmq',
      'sources': [ 'binding.cc' ],
      'conditions': [
        ['OS=="win"', {
          'include_dirs': ['windows/include'],
          'link_settings': {
            'libraries': [
              'Delayimp.lib',
            ],
            'conditions': [
              ['target_arch=="ia32"', {
                'libraries': [
                  '<(PRODUCT_DIR)/../../windows/lib/x86/libzmq-v100-mt-3_2_2.lib',
                ]
              },{
                'libraries': [
                  '<(PRODUCT_DIR)/../../windows/lib/x64/libzmq-v100-mt-3_2_2.lib',
                ]
              }]
            ],
          },
          'msvs_settings': {
            'VCLinkerTool': {
              'DelayLoadDLLs': ['libzmq-v100-mt-3_2_2.dll']
            }
          },
        }, {
          'libraries': ['-lzmq'],
          'cflags!': ['-fno-exceptions'],
          'cflags_cc!': ['-fno-exceptions'],
        }],
        ['OS=="mac" or OS=="solaris"', {
          'xcode_settings': {
            'GCC_ENABLE_CPP_EXCEPTIONS': 'YES'
          },
          # add macports include & lib dirs
          'include_dirs': [ "../../../zeromq/x64-osx/include" ],
          'libraries': [ "-L../../../../zeromq/x64-osx/lib" ]
        }],
        ['OS=="openbsd" or OS=="freebsd"', {
          'include_dirs': [
            '<!@(pkg-config libzmq --cflags-only-I | sed s/-I//g)',
            '/usr/local/include',
          ],
          'libraries': [
            '<!@(pkg-config libzmq --libs)',
            '-L/usr/local/lib',
          ]
        }],
        ['OS=="linux" and target_arch!="ia32"', {
          'include_dirs': [ "../../../zeromq/x64-linux/include" ],
          'libraries' : [ "-L../../../../zeromq/x64-linux/lib", "-L../../../../util-linux/x64-linux/lib", "-luuid"],
        }],
        ['OS=="linux" and target_arch=="ia32"', {
          'include_dirs': [ "../../../zeromq/x86-linux/include" ],
          'libraries' : [ "-L../../../../zeromq/x86-linux/lib", "-L../../../../util-linux/x64-linux/lib", "-luuid"],
        }],
      ]
    }
  ]
}